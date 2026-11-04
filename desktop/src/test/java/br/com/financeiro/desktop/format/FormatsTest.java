package br.com.financeiro.desktop.format;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.util.StringConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FormatsTest {

    private final StringConverter<LocalDate> converter = Formats.dateConverter();

    /** O Java separa o símbolo do número com um espaço que não quebra a linha. */
    private static String withoutSpecialSpace(String text) {
        return text.replace(' ', ' ');
    }

    @Test
    void formatsCurrencyInBrazilianStyle() {
        assertThat(withoutSpecialSpace(Formats.currency(new BigDecimal("1234.5")))).isEqualTo("R$ 1.234,50");
        assertThat(withoutSpecialSpace(Formats.currency(new BigDecimal("0.00")))).isEqualTo("R$ 0,00");
    }

    @Test
    void keepsEveryDigitOfLargeValues() {
        // Passar por double perderia os centavos em um valor deste tamanho (RNF-06).
        assertThat(withoutSpecialSpace(Formats.currency(new BigDecimal("9999999999999.99"))))
                .isEqualTo("R$ 9.999.999.999.999,99");
    }

    @Test
    void formatsDateAsDayMonthYear() {
        assertThat(Formats.date(LocalDate.of(2026, 3, 5))).isEqualTo("05/03/2026");
        assertThat(converter.toString(LocalDate.of(2026, 3, 5))).isEqualTo("05/03/2026");
        assertThat(converter.toString(null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"05/03/2026", "5/3/2026", " 05/03/2026 "})
    void readsTypedDates(String text) {
        assertThat(converter.fromString(text)).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "31/02/2026", "2026-03-05", "ontem", "32/01/2026"})
    void invalidTypedDatesBecomeEmpty(String text) {
        assertThat(converter.fromString(text)).isNull();
    }
}
