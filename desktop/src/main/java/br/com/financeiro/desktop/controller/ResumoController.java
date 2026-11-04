package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.dto.CategoriaTotalResponse;
import br.com.financeiro.desktop.api.dto.SaldoResponse;
import br.com.financeiro.desktop.format.Formats;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Resumo de um período: saldo (RF-16) e total por categoria (RF-17). Todos os
 * cálculos vêm da API. Abre no mês atual.
 */
public class ResumoController {

    private static final Map<String, String> LABELS = Map.of("inicio", "Início", "fim", "Fim");
    private static final String EMPTY_AMOUNT = "—";
    private static final String NEGATIVE_STYLE = "amount-expense";

    private final ApiClient api;
    private final ApiTasks tasks;

    @FXML
    private DatePicker startPicker;
    @FXML
    private DatePicker endPicker;
    @FXML
    private Label errorLabel;
    @FXML
    private Label revenueLabel;
    @FXML
    private Label expenseLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private TableView<CategoriaTotalResponse> table;
    @FXML
    private Label placeholderLabel;
    @FXML
    private TableColumn<CategoriaTotalResponse, String> categoryColumn;
    @FXML
    private TableColumn<CategoriaTotalResponse, String> typeColumn;
    @FXML
    private TableColumn<CategoriaTotalResponse, String> countColumn;
    @FXML
    private TableColumn<CategoriaTotalResponse, CategoriaTotalResponse> totalColumn;

    private long lastRequest;

    /**
     * Cria o controller.
     *
     * @param api   cliente da API
     * @param tasks executor das chamadas
     */
    public ResumoController(ApiClient api, ApiTasks tasks) {
        this.api = api;
        this.tasks = tasks;
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        startPicker.setConverter(Formats.dateConverter());
        endPicker.setConverter(Formats.dateConverter());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        bindText(categoryColumn, CategoriaTotalResponse::nome);
        bindText(typeColumn, item -> item.tipo().label());
        bindText(countColumn, item -> String.valueOf(item.quantidade()));
        totalColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        totalColumn.setCellFactory(column ->
                new AmountCell<>(CategoriaTotalResponse::total, CategoriaTotalResponse::tipo));

        YearMonth month = YearMonth.now();
        startPicker.setValue(month.atDay(1));
        endPicker.setValue(month.atEndOfMonth());
        load();
    }

    @FXML
    private void onUpdate() {
        if (DateFields.commitTyped(startPicker) && DateFields.commitTyped(endPicker)) {
            load();
        } else {
            showFailure("Data inválida. Use o formato dd/mm/aaaa.");
        }
    }

    private static void bindText(
            TableColumn<CategoriaTotalResponse, String> column, Function<CategoriaTotalResponse, String> text) {
        column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(text.apply(row.getValue())));
    }

    private void load() {
        // Uma resposta lenta de um período antigo não pode sobrescrever a do período atual.
        long request = ++lastRequest;
        LocalDate start = startPicker.getValue();
        LocalDate end = endPicker.getValue();
        errorLabel.setVisible(false);
        tasks.run(() -> new Summary(api.getSaldo(start, end), api.listCategoriaTotals(start, end)),
                summary -> {
                    if (request == lastRequest) {
                        show(summary);
                    }
                },
                failure -> {
                    if (request == lastRequest) {
                        showFailure(failure.describe(LABELS));
                    }
                });
    }

    private void show(Summary summary) {
        placeholderLabel.setText("Nenhum lançamento no período.");
        revenueLabel.setText(Formats.currency(summary.balance().totalReceitas()));
        expenseLabel.setText(Formats.currency(summary.balance().totalDespesas()));
        balanceLabel.setText(Formats.currency(summary.balance().saldo()));
        balanceLabel.getStyleClass().remove(NEGATIVE_STYLE);
        if (summary.balance().saldo().signum() < 0) {
            balanceLabel.getStyleClass().add(NEGATIVE_STYLE);
        }
        table.getItems().setAll(summary.totals());
    }

    /** Os números antigos saem da tela: pareceriam ser do período que falhou. */
    private void showFailure(String message) {
        revenueLabel.setText(EMPTY_AMOUNT);
        expenseLabel.setText(EMPTY_AMOUNT);
        balanceLabel.setText(EMPTY_AMOUNT);
        balanceLabel.getStyleClass().remove(NEGATIVE_STYLE);
        table.getItems().clear();
        placeholderLabel.setText("Não foi possível carregar o resumo.");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private record Summary(SaldoResponse balance, List<CategoriaTotalResponse> totals) {
    }
}
