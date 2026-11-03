package com.portodigital.validacaodocumentos.repository;

import com.portodigital.validacaodocumentos.domain.entity.Documento;
import com.portodigital.validacaodocumentos.domain.entity.TextoBrutoOcr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para gerenciamento e persistência de {@link TextoBrutoOcr}.
 */
@Repository
public interface TextoBrutoOcrRepository extends JpaRepository<TextoBrutoOcr, UUID> {

    Optional<TextoBrutoOcr> findByDocumentoId(UUID documentoId);

    Optional<TextoBrutoOcr> findByDocumento(Documento documento);
}

