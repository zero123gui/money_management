package br.com.financeiro.desktop.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financeiro.desktop.api.dto.LancamentoResponse;
import br.com.financeiro.desktop.api.dto.Tipo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LancamentoFilterTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 15);
    private static final LancamentoResponse ENTRY = new LancamentoResponse(
            1L, Tipo.DESPESA, new BigDecimal("10.00"), DAY, null,
            new LancamentoResponse.CategoriaResumo(7L, "Moradia"), Instant.EPOCH, Instant.EPOCH);

    @Test
    void emptyFilterMatchesEverything() {
        assertThat(new LancamentoFilter(null, null, null, null).matches(ENTRY)).isTrue();
    }

    @Test
    void periodIncludesBothEnds() {
        assertThat(new LancamentoFilter(DAY, DAY, null, null).matches(ENTRY)).isTrue();
        assertThat(new LancamentoFilter(DAY.plusDays(1), null, null, null).matches(ENTRY)).isFalse();
        assertThat(new LancamentoFilter(null, DAY.minusDays(1), null, null).matches(ENTRY)).isFalse();
    }

    @Test
    void typeAndCategoryMustBeTheSame() {
        assertThat(new LancamentoFilter(null, null, Tipo.DESPESA, 7L).matches(ENTRY)).isTrue();
        assertThat(new LancamentoFilter(null, null, Tipo.RECEITA, null).matches(ENTRY)).isFalse();
        assertThat(new LancamentoFilter(null, null, null, 8L).matches(ENTRY)).isFalse();
    }
}
