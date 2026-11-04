package br.com.financeiro.backend.dto;

import br.com.financeiro.backend.entity.Tipo;
import java.math.BigDecimal;

/**
 * Item de {@code GET /relatorios/categorias}.
 *
 * @param categoriaId identificador da categoria
 * @param nome        nome da categoria
 * @param tipo        tipo da categoria
 * @param total       soma dos lançamentos no período
 * @param quantidade  número de lançamentos no período
 */
public record ResumoCategoriaResponse(Long categoriaId, String nome, Tipo tipo, BigDecimal total, long quantidade) {
}
