package com.evox.backend.service;

import com.evox.backend.dto.CarritoResponse;
import com.evox.backend.dto.ItemCarritoResponse;
import com.evox.backend.exception.ApiException;
import com.evox.backend.model.Carrito;
import com.evox.backend.model.Perfil;
import com.evox.backend.model.Producto;
import com.evox.backend.repository.CarritoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ProductoService productoService;

    public CarritoService(CarritoRepository carritoRepository, ProductoService productoService) {
        this.carritoRepository = carritoRepository;
        this.productoService = productoService;
    }

    /** GET /api/v1/carrito */
    public CarritoResponse verCarrito(Perfil usuario) {
        return construirRespuesta(carritoRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId()));
    }

    /** POST /api/v1/carrito/items */
    public CarritoResponse agregarProducto(Perfil usuario, UUID productoId, int cantidad) {
        Producto producto = productoService.buscarEntidad(productoId);

        Carrito fila = carritoRepository.findByUsuarioIdAndProductoId(usuario.getId(), productoId)
                .orElse(null);

        if (fila == null) {
            fila = new Carrito();
            fila.setUsuario(usuario);
            fila.setProducto(producto);
            fila.setCantidad(cantidad);
            fila.setPrecioUnitario(producto.getPrecio());
        } else {
            fila.setCantidad(fila.getCantidad() + cantidad);
        }

        validarStock(producto, fila.getCantidad());
        carritoRepository.save(fila);

        return construirRespuesta(carritoRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId()));
    }

    /** PUT /api/v1/carrito/items/{productoId} */
    public CarritoResponse actualizarCantidad(Perfil usuario, UUID productoId, int cantidad) {
        Carrito fila = carritoRepository.findByUsuarioIdAndProductoId(usuario.getId(), productoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El producto no esta en el carrito"));

        validarStock(fila.getProducto(), cantidad);
        fila.setCantidad(cantidad);
        carritoRepository.save(fila);

        return construirRespuesta(carritoRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId()));
    }

    /** DELETE /api/v1/carrito/items/{productoId} */
    @org.springframework.transaction.annotation.Transactional
    public void eliminarProducto(Perfil usuario, UUID productoId) {
        Carrito fila = carritoRepository.findByUsuarioIdAndProductoId(usuario.getId(), productoId)
                .orElseThrow(() -> new com.evox.backend.exception.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "El producto no esta en el carrito"));
        carritoRepository.delete(fila);
    }

    private void validarStock(Producto producto, int cantidadDeseada) {
        if (cantidadDeseada > producto.getStock()) {
            throw new ApiException(HttpStatus.CONFLICT, "Stock insuficiente");
        }
    }

    private CarritoResponse construirRespuesta(List<Carrito> lineas) {
        List<ItemCarritoResponse> items = lineas.stream()
                .map(l -> new ItemCarritoResponse(
                        l.getProducto().getId(),
                        l.getProducto().getNombre(),
                        l.getPrecioUnitario(),
                        l.getCantidad(),
                        l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad()))
                ))
                .toList();

        BigDecimal total = items.stream()
                .map(ItemCarritoResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarritoResponse(items, total);
    }
}