package com.portodigital.validacaodocumentos.repository;

import com.portodigital.validacaodocumentos.domain.entity.Documento;
import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
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
 * Repositório JPA para a entidade {@link Documento}.
 *
 * <p>
 * Utiliza {@code @EntityGraph} para carregar os relacionamentos
 * {@code dadosExtraidos}
 * e {@code validacao} de forma eager em queries específicas, evitando o
 * problema N+1
 * sem impactar o comportamento padrão LAZY das associações {@code @OneToOne}.
 *
 * <p>
 * A query {@code findAllWithFilter} usa JPQL compatível com H2 (testes) e
 * PostgreSQL
 * (produção), aceitando {@code null} como parâmetro {@code status} para
 * retornar todos os documentos
 * sem filtro de status.
 */
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    /**
     * Retorna todos os documentos com seus relacionamentos carregados (sem
     * paginação).
     * Usado internamente para verificações e relatórios.
     */
    @Query("SELECT d FROM Documento d LEFT JOIN FETCH d.dadosExtraidos LEFT JOIN FETCH d.validacao ORDER BY d.dataUpload DESC")
    List<Documento> findAllWithDetails();

    /**
     * Busca um documento por ID carregando eagerly os relacionamentos
     * {@code dadosExtraidos}
     * e {@code validacao} via {@code @EntityGraph}.
     *
     * @param id Identificador UUID do documento
     * @return Optional com o documento completo ou vazio se não encontrado
     */
    @EntityGraph(attributePaths = { "dadosExtraidos", "validacao" })
    @Query("SELECT d FROM Documento d WHERE d.id = :id")
    Optional<Documento> findWithDetailsById(@Param("id") UUID id);

    /**
     * Lista paginada de documentos com filtro opcional por status de validação.
     *
     * <p>
     * Quando {@code status} é {@code null}, todos os documentos são retornados
     * independente do status.
     * Quando {@code status} é informado, apenas os documentos cuja validação possui
     * o status correspondente
     * são retornados.
     *
     * <p>
     * <strong>Compatibilidade H2/PostgreSQL:</strong> a expressão
     * {@code (:status IS NULL OR v.status = :status)}
     * funciona corretamente em ambos os dialetos JPQL. Para enums tipados, o
     * Hibernate converte
     * automaticamente o parâmetro para o tipo {@link StatusValidacao}.
     *
     * @param status   Filtro opcional por {@link StatusValidacao} (APROVADO /
     *                 REPROVADO)
     * @param pageable Configuração de paginação e ordenação
     * @return Página de documentos que satisfazem o filtro
     */
    @EntityGraph(attributePaths = { "dadosExtraidos", "validacao" })
    @Query(value = "SELECT d FROM Documento d LEFT JOIN d.validacao v " +
            "WHERE (:status IS NULL OR v.status = :status)", countQuery = "SELECT COUNT(d) FROM Documento d LEFT JOIN d.validacao v "
                    +
                    "WHERE (:status IS NULL OR v.status = :status)")
    Page<Documento> findAllWithFilter(@Param("status") StatusValidacao status, Pageable pageable);
}
