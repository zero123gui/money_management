package br.com.financeiro.backend.dto;

import br.com.financeiro.backend.entity.Lancamento;
import br.com.financeiro.backend.entity.Tipo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Lançamento ativo devolvido pela API.
 *
 * @param id           identificador do lançamento
 * @param tipo         tipo do lançamento
 * @param valor        valor positivo
 * @param data         data do lançamento
 * @param descricao    descrição, ou {@code null} se não houver
 * @param categoria    categoria do lançamento
 * @param criadoEm     data e hora de criação, em UTC
 * @param atualizadoEm data e hora da última alteração, em UTC
 */
public record LancamentoResponse(Long id, Tipo tipo, BigDecimal valor, LocalDate data, String descricao,
        CategoriaResumo categoria, Instant criadoEm, Instant atualizadoEm) {

    /**
     * Categoria dentro de um lançamento.
     *
     * @param id   identificador da categoria
     * @param nome nome da categoria
     */
    public record CategoriaResumo(Long id, String nome) {
    }

    /**
     * Monta a resposta a partir da entidade.
     *
     * @param lancamento lançamento a expor
     * @return os dados do lançamento
     */
    public static LancamentoResponse from(Lancamento lancamento) {
        return new LancamentoResponse(lancamento.getId(), lancamento.getType(), lancamento.getAmount(),
                lancamento.getDate(), lancamento.getDescription(),
                new CategoriaResumo(lancamento.getCategory().getId(), lancamento.getCategory().getName()),
                lancamento.getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
                lancamento.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}
