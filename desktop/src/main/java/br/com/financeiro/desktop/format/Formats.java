package br.com.financeiro.desktop.format;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import javafx.util.StringConverter;

/**
 * Formatos brasileiros de exibição (RNF-07): a API só trabalha com ISO 8601 e
 * números puros.
 */
public final class Formats {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_OUTPUT = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    // STRICT recusa datas como 31/02, que o modo padrão trocaria pelo último dia do mês.
    private static final DateTimeFormatter DATE_INPUT =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Formats() {
    }

    /**
     * Formata um valor em reais, como {@code R$ 1.234,56}.
     *
     * @param value valor
     * @return valor formatado
     */
    public static String currency(BigDecimal value) {
        // NumberFormat não é seguro entre threads, por isso não fica em uma constante.
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }

    /**
     * Formata uma data como {@code dd/MM/aaaa}.
     *
     * @param value data
     * @return data formatada
     */
    public static String date(LocalDate value) {
        return DATE_OUTPUT.format(value);
    }

    /**
     * Cria o conversor dos campos de data. Um texto inválido vira campo vazio, em
     * vez de uma exceção dentro do JavaFX que deixaria o valor antigo na tela.
     *
     * @return conversor entre data e texto
     */
    public static StringConverter<LocalDate> dateConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return value == null ? "" : date(value);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(text.strip(), DATE_INPUT);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        };
    }
}
