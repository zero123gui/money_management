package br.com.financeiro.backend.dto;

import br.com.financeiro.backend.dto.LancamentoResponse.CategoriaResumo;
import br.com.financeiro.backend.entity.Lancamento;
import br.com.financeiro.backend.entity.Tipo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Item de {@code GET /lancamentos/excluidos}: um lançamento mais a data da exclusão.
 *
 * @param id           identificador do lançamento
 * @param tipo         tipo do lançamento
 * @param valor        valor positivo
 * @param data         data do lançamento
 * @param descricao    descrição, ou {@code null} se não houver
 * @param categoria    categoria do lançamento
 * @param criadoEm     data e hora de criação, em UTC
 * @param atualizadoEm data e hora da última alteração, em UTC
 * @param excluidoEm   data e hora da exclusão, em UTC
 */
public record LancamentoExcluidoResponse(Long id, Tipo tipo, BigDecimal valor, LocalDate data, String descricao,
        CategoriaResumo categoria, Instant criadoEm, Instant atualizadoEm, Instant excluidoEm) {

    /**
     * Monta a resposta a partir da entidade.
     *
     * @param lancamento lançamento excluído a expor
     * @return os dados do lançamento
     */
    public static LancamentoExcluidoResponse from(Lancamento lancamento) {
        LancamentoResponse base = LancamentoResponse.from(lancamento);
        return new LancamentoExcluidoResponse(base.id(), base.tipo(), base.valor(), base.data(), base.descricao(),
                base.categoria(), base.criadoEm(), base.atualizadoEm(),
                lancamento.getDeletedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}
