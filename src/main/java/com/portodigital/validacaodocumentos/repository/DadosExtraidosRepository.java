package com.portodigital.validacaodocumentos.repository;

import com.portodigital.validacaodocumentos.domain.entity.DadosExtraidos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DadosExtraidosRepository extends JpaRepository<DadosExtraidos, UUID> {
    Optional<DadosExtraidos> findByDocumentoId(UUID documentoId);
}
