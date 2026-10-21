package com.portodigital.validacaodocumentos.repository;

import com.portodigital.validacaodocumentos.domain.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    @Query("SELECT d FROM Documento d LEFT JOIN FETCH d.dadosExtraidos LEFT JOIN FETCH d.validacao ORDER BY d.dataUpload DESC")
    List<Documento> findAllWithDetails();
}
