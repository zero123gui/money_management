package br.com.financeiro.desktop.api;

import br.com.financeiro.desktop.api.dto.LancamentoResponse;
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

    /**
     * Diz se um lançamento recém-salvo aparece com estes filtros. Serve só para
     * avisar o usuário: quem decide o que é listado continua sendo a API.
     *
     * @param entry lançamento
     * @return {@code true} se ele passa por todos os filtros
     */
    public boolean matches(LancamentoResponse entry) {
        return (inicio == null || !entry.data().isBefore(inicio))
                && (fim == null || !entry.data().isAfter(fim))
                && (tipo == null || entry.tipo() == tipo)
                && (categoriaId == null || categoriaId.equals(entry.categoria().id()));
    }
}
