package br.com.financeiro.desktop.api;

import br.com.financeiro.desktop.api.dto.Tipo;
import java.time.LocalDate;

/**
 * Filtros de {@code GET /lancamentos}. Um valor {@code null} não filtra.
 *
 * @param inicio      primeira data do período, inclusive
 * @param fim         última data do período, inclusive
 * @param tipo        receita ou despesa
 * @param categoriaId categoria
 */
public record LancamentoFilter(LocalDate inicio, LocalDate fim, Tipo tipo, Long categoriaId) {
}
