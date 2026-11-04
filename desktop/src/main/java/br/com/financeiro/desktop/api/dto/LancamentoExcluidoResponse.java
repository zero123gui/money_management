package br.com.financeiro.desktop.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Item de {@code GET /lancamentos/excluidos}.
 *
 * @param id           identificador
 * @param tipo         receita ou despesa
 * @param valor        valor, sempre positivo
 * @param data         data do lançamento
 * @param descricao    descrição, ou {@code null}
 * @param categoria    categoria do lançamento
 * @param criadoEm     data e hora de criação, em UTC
 * @param atualizadoEm data e hora da última alteração, em UTC
 * @param excluidoEm   data e hora da exclusão, em UTC
 */
public record LancamentoExcluidoResponse(
        Long id, Tipo tipo, BigDecimal valor, LocalDate data, String descricao,
        LancamentoResponse.CategoriaResumo categoria, Instant criadoEm, Instant atualizadoEm, Instant excluidoEm) {
}
