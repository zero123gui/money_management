package br.com.financeiro.desktop.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Corpo de {@code POST /lancamentos} e {@code PUT /lancamentos/{id}}. Um campo
 * obrigatório vazio vai como {@code null}, e a API responde qual falta.
 *
 * @param tipo        receita ou despesa
 * @param valor       valor
 * @param data        data do lançamento
 * @param descricao   descrição
 * @param categoriaId categoria
 */
public record LancamentoRequest(Tipo tipo, BigDecimal valor, LocalDate data, String descricao, Long categoriaId) {
}
