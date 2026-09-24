package com.evox.backend.repository;

import com.evox.backend.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarritoRepository extends JpaRepository<Carrito, UUID> {
    List<Carrito> findByUsuarioIdOrderByFechaDesc(UUID usuarioId);
    Optional<Carrito> findByUsuarioIdAndProductoId(UUID usuarioId, UUID productoId);
    @Modifying
    @Transactional
    void deleteByUsuarioId(UUID usuarioId);
    @Modifying
    @Transactional
    void deleteByUsuarioIdAndProductoId(UUID usuarioId, UUID productoId);
}