package com.portodigital.validacaodocumentos.repository;

import com.portodigital.validacaodocumentos.domain.entity.Validacao;
import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ValidacaoRepository extends JpaRepository<Validacao, UUID> {
    Optional<Validacao> findByDocumentoId(UUID documentoId);
    List<Validacao> findByStatus(StatusValidacao status);
}
