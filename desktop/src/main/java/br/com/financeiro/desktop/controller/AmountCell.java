package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.dto.Tipo;
import br.com.financeiro.desktop.format.Formats;
import java.math.BigDecimal;
import java.util.function.Function;
import javafx.scene.control.TableCell;

/**
 * Célula de tabela com um valor em reais, à direita, na cor do tipo.
 *
 * @param <T> tipo das linhas da tabela
 */
final class AmountCell<T> extends TableCell<T, T> {

    private static final String REVENUE_STYLE = "amount-revenue";
    private static final String EXPENSE_STYLE = "amount-expense";

    private final Function<T, BigDecimal> amount;
    private final Function<T, Tipo> type;

    /**
     * Cria a célula.
     *
     * @param amount lê o valor da linha
     * @param type   lê o tipo da linha
     */
    AmountCell(Function<T, BigDecimal> amount, Function<T, Tipo> type) {
        this.amount = amount;
        this.type = type;
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().removeAll(REVENUE_STYLE, EXPENSE_STYLE);
        if (empty || item == null) {
            setText(null);
        } else {
            setText(Formats.currency(amount.apply(item)));
            getStyleClass().add(type.apply(item) == Tipo.RECEITA ? REVENUE_STYLE : EXPENSE_STYLE);
        }
    }
}
