package com.portodigital.validacaodocumentos.repository;

import com.portodigital.validacaodocumentos.domain.entity.Documento;
import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para a entidade raiz {@link Documento}.
 */
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    /**
     * Busca paginada de documentos filtrando pelo status de validação.
     * Atende ao requisito do pipeline: Page<Documento> findByStatus(StatusValidacao status, Pageable pageable).
     *
     * @param status   Status da validação (APROVADO, REPROVADO, EM_PROCESSAMENTO)
     * @param pageable Configuração de paginação e ordenação
     * @return Página de documentos com o status informado
     */
    @EntityGraph(attributePaths = {"dadosExtraidos", "validacao", "textoBrutoOcr"})
    Page<Documento> findByStatus(StatusValidacao status, Pageable pageable);

    /**
     * Busca paginada de todos os documentos carregando os relacionamentos via EntityGraph.
     */
    @Override
    @EntityGraph(attributePaths = {"dadosExtraidos", "validacao", "textoBrutoOcr"})
    Page<Documento> findAll(Pageable pageable);

    /**
     * Busca documento por ID carregando eager todos os relacionamentos para visualização detalhada.
     */
    @EntityGraph(attributePaths = {"dadosExtraidos", "validacao", "textoBrutoOcr"})
    @Query("SELECT d FROM Documento d WHERE d.id = :id")
    Optional<Documento> findWithDetailsById(@Param("id") UUID id);

    /**
     * Busca paginada com filtro opcional por status.
     * Quando status é null, retorna todos os documentos; quando informado, filtra pelo status correspondente.
     */
    @EntityGraph(attributePaths = {"dadosExtraidos", "validacao", "textoBrutoOcr"})
    @Query(value = "SELECT d FROM Documento d WHERE (:status IS NULL OR d.status = :status)",
           countQuery = "SELECT COUNT(d) FROM Documento d WHERE (:status IS NULL OR d.status = :status)")
    Page<Documento> findAllWithFilter(@Param("status") StatusValidacao status, Pageable pageable);

    /**
     * Lista documentos pelo tipo de documento (COMPROVANTE_RESIDENCIA, RG, CNH).
     */
    List<Documento> findByTipoDocumento(TipoDocumento tipoDocumento);
}
