package br.com.financeiro.backend.repository;

import br.com.financeiro.backend.entity.Tipo;
import java.math.BigDecimal;

/**
 * Soma dos lançamentos de um tipo.
 *
 * @param type  tipo dos lançamentos
 * @param total soma dos valores
 */
public record TypeTotal(Tipo type, BigDecimal total) {
}
