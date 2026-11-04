package br.com.financeiro.desktop.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta de {@code GET /relatorios/saldo}.
 *
 * @param inicio        primeira data do período
 * @param fim           última data do período
 * @param totalReceitas soma das receitas
 * @param totalDespesas soma das despesas
 * @param saldo         receitas menos despesas; pode ser negativo
 */
public record SaldoResponse(
        LocalDate inicio, LocalDate fim, BigDecimal totalReceitas, BigDecimal totalDespesas, BigDecimal saldo) {
}
