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
    void formatsAmountForEditing() {
        assertThat(Formats.amount(new BigDecimal("1234.5"))).isEqualTo("1.234,50");
        assertThat(Formats.amount(new BigDecimal("9999999999999.99"))).isEqualTo("9.999.999.999.999,99");
    }

    @Test
    void readsTypedAmounts() {
        assertThat(Formats.parseAmount("1.234,56")).isEqualTo(new BigDecimal("1234.56"));
        assertThat(Formats.parseAmount("1234,5")).isEqualTo(new BigDecimal("1234.50"));
        assertThat(Formats.parseAmount(" 10 ")).isEqualTo(new BigDecimal("10.00"));
        assertThat(Formats.parseAmount("1.234")).isEqualTo(new BigDecimal("1234.00"));
        assertThat(Formats.parseAmount("1.000.000,01")).isEqualTo(new BigDecimal("1000000.01"));
        assertThat(Formats.parseAmount("0,00")).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void amountThatWasFormattedCanBeReadBack() {
        BigDecimal value = new BigDecimal("9999999999999.99");

        assertThat(Formats.parseAmount(Formats.amount(value))).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.50", "1,234", "1.23,45", "12.34.567", "abc", "-10", "1 000", "R$ 10", ",50", "10,"})
    void refusesAmountsThatCouldBeMisread(String text) {
        assertThat(Formats.parseAmount(text)).isNull();
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
