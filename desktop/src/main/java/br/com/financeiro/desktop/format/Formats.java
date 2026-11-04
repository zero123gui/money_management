package br.com.financeiro.desktop.format;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Pattern;
import javafx.util.StringConverter;

/**
 * Formatos brasileiros de exibição (RNF-07): a API só trabalha com ISO 8601 e
 * números puros.
 */
public final class Formats {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_OUTPUT = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter DATE_TIME_OUTPUT = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    // STRICT recusa datas como 31/02, que o modo padrão trocaria pelo último dia do mês.
    private static final DateTimeFormatter DATE_INPUT =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Só aceita milhares em grupos de 3 e até 2 decimais depois da vírgula. "10.50" e "1,234"
    // são recusados em vez de adivinhados, pois lidos errado virariam outro valor.
    private static final Pattern AMOUNT_INPUT = Pattern.compile("\\d{1,3}(\\.\\d{3})+(,\\d{1,2})?|\\d+(,\\d{1,2})?");

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
     * Formata um valor para um campo editável, como {@code 1.234,56}.
     *
     * @param value valor
     * @return valor formatado, sem o símbolo da moeda
     */
    public static String amount(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    /**
     * Lê um valor digitado no formato brasileiro, como {@code 1.234,56} ou {@code 1234,5}.
     *
     * @param text texto digitado
     * @return valor com 2 casas decimais, ou {@code null} se o formato for inválido
     */
    public static BigDecimal parseAmount(String text) {
        String value = text.strip();
        if (!AMOUNT_INPUT.matcher(value).matches()) {
            return null;
        }
        return new BigDecimal(value.replace(".", "").replace(',', '.')).setScale(2);
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
     * Formata uma data e hora da API, que vem em UTC, no fuso deste computador.
     *
     * @param value data e hora
     * @return texto como {@code 04/11/2026 13:30}
     */
    public static String dateTime(Instant value) {
        return dateTime(value, ZoneId.systemDefault());
    }

    /**
     * Formata uma data e hora em um fuso específico.
     *
     * @param value data e hora
     * @param zone  fuso de exibição
     * @return texto como {@code 04/11/2026 13:30}
     */
    public static String dateTime(Instant value, ZoneId zone) {
        return DATE_TIME_OUTPUT.format(value.atZone(zone));
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
