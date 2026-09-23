package com.evox.backend.controller;

import com.evox.backend.dto.CantidadRequest;
import com.evox.backend.dto.CarritoResponse;
import com.evox.backend.dto.ItemCarritoRequest;
import com.evox.backend.dto.MensajeResponse;
import com.evox.backend.security.UsuarioActual;
import com.evox.backend.service.CarritoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Endpoints del carrito de compras (requiere estar autenticado como cliente). */
@RestController
@RequestMapping("/api/v1/carrito")
public class CarritoController {

    private final CarritoService carritoService;
    private final UsuarioActual usuarioActual;

    public CarritoController(CarritoService carritoService, UsuarioActual usuarioActual) {
        this.carritoService = carritoService;
        this.usuarioActual = usuarioActual;
    }

    // GET /api/v1/carrito
    @GetMapping
    public ResponseEntity<CarritoResponse> ver() {
        return ResponseEntity.ok(carritoService.verCarrito(usuarioActual.obtener()));
    }

    // POST /api/v1/carrito/items
    @PostMapping("/items")
    public ResponseEntity<?> agregar(@Valid @RequestBody ItemCarritoRequest datos) {
        CarritoResponse carrito = carritoService.agregarProducto(
                usuarioActual.obtener(), datos.getProductoId(), datos.getCantidad());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RespuestaCarrito("Producto agregado al carrito", carrito.getTotal()));
    }

    // PUT /api/v1/carrito/items/{productoId}
    @PutMapping("/items/{productoId}")
    public ResponseEntity<?> actualizarCantidad(@PathVariable UUID productoId, @Valid @RequestBody CantidadRequest datos) {
        CarritoResponse carrito = carritoService.actualizarCantidad(usuarioActual.obtener(), productoId, datos.getCantidad());

        // Se busca el subtotal del producto actualizado para incluirlo en la respuesta.
        var itemActualizado = carrito.getItems().stream()
                .filter(i -> i.getProductoId().equals(productoId))
                .findFirst()
                .orElseThrow(() -> new com.evox.backend.exception.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "El producto no esta en el carrito"));

        return ResponseEntity.ok(new RespuestaCantidadActualizada(
                "Cantidad actualizada", itemActualizado.getSubtotal(), carrito.getTotal()));
    }

    // DELETE /api/v1/carrito/items/{productoId}
    @DeleteMapping("/items/{productoId}")
    public ResponseEntity<MensajeResponse> eliminar(@PathVariable UUID productoId) {
        carritoService.eliminarProducto(usuarioActual.obtener(), productoId);
        return ResponseEntity.ok(new MensajeResponse("Producto eliminado del carrito"));
    }

    // Pequenos registros (records) solo para dar la forma exacta de respuesta que pide el contrato.
    record RespuestaCarrito(String mensaje, java.math.BigDecimal total) {}
    record RespuestaCantidadActualizada(String mensaje, java.math.BigDecimal subtotal, java.math.BigDecimal total) {}
}