package com.portodigital.validacaodocumentos.repository;

import com.portodigital.validacaodocumentos.domain.entity.DadosExtraidos;
import com.portodigital.validacaodocumentos.domain.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para gerenciamento e persistência de {@link DadosExtraidos}.
 */
@Repository
public interface DadosExtraidosRepository extends JpaRepository<DadosExtraidos, UUID> {

    Optional<DadosExtraidos> findByDocumentoId(UUID documentoId);

    Optional<DadosExtraidos> findByDocumento(Documento documento);
}
