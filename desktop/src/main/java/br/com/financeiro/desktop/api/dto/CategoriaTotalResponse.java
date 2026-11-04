package br.com.financeiro.desktop.api.dto;

import java.math.BigDecimal;

/**
 * Item de {@code GET /relatorios/categorias}.
 *
 * @param categoriaId identificador da categoria
 * @param nome        nome da categoria
 * @param tipo        sentido dos lançamentos da categoria
 * @param total       soma dos lançamentos no período
 * @param quantidade  quantidade de lançamentos no período
 */
public record CategoriaTotalResponse(Long categoriaId, String nome, Tipo tipo, BigDecimal total, long quantidade) {
}
