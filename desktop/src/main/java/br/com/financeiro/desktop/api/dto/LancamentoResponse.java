package br.com.financeiro.desktop.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lançamento devolvido pela API.
 *
 * @param id           identificador
 * @param tipo         receita ou despesa
 * @param valor        valor, sempre positivo
 * @param data         data do lançamento
 * @param descricao    descrição, ou {@code null}
 * @param categoria    categoria do lançamento
 * @param criadoEm     data e hora de criação, em UTC
 * @param atualizadoEm data e hora da última alteração, em UTC
 */
public record LancamentoResponse(
        Long id, Tipo tipo, BigDecimal valor, LocalDate data, String descricao,
        CategoriaResumo categoria, Instant criadoEm, Instant atualizadoEm) {

    /**
     * Categoria resumida, dentro de um lançamento.
     *
     * @param id   identificador
     * @param nome nome
     */
    public record CategoriaResumo(Long id, String nome) {
    }
}
