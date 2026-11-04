package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiException;
import br.com.financeiro.desktop.api.LancamentoFilter;
import br.com.financeiro.desktop.api.dto.CategoriaResponse;
import br.com.financeiro.desktop.api.dto.LancamentoResponse;
import br.com.financeiro.desktop.api.dto.PageResponse;
import br.com.financeiro.desktop.api.dto.Tipo;
import br.com.financeiro.desktop.format.Formats;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Lista de lançamentos, com filtros e paginação (RF-10). Abre no mês atual.
 */
public class LancamentosController {

    private static final int PAGE_SIZE = 50;
    private static final String REVENUE_STYLE = "amount-revenue";
    private static final String EXPENSE_STYLE = "amount-expense";

    private final ApiClient api;
    private final ApiTasks tasks;

    @FXML
    private DatePicker startPicker;
    @FXML
    private DatePicker endPicker;
    @FXML
    private ComboBox<Option<Tipo>> typeBox;
    @FXML
    private ComboBox<Option<Long>> categoryBox;
    @FXML
    private Label errorLabel;
    @FXML
    private TableView<LancamentoResponse> table;
    @FXML
    private Label placeholderLabel;
    @FXML
    private TableColumn<LancamentoResponse, String> dateColumn;
    @FXML
    private TableColumn<LancamentoResponse, String> descriptionColumn;
    @FXML
    private TableColumn<LancamentoResponse, String> categoryColumn;
    @FXML
    private TableColumn<LancamentoResponse, String> typeColumn;
    @FXML
    private TableColumn<LancamentoResponse, LancamentoResponse> amountColumn;
    @FXML
    private Label pageLabel;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;

    private List<CategoriaResponse> categories = List.of();
    private LancamentoFilter appliedFilter;
    private int currentPage;
    private long lastRequest;

    /**
     * Cria o controller.
     *
     * @param api   cliente da API
     * @param tasks executor das chamadas
     */
    public LancamentosController(ApiClient api, ApiTasks tasks) {
        this.api = api;
        this.tasks = tasks;
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        startPicker.setConverter(Formats.dateConverter());
        endPicker.setConverter(Formats.dateConverter());
        configureColumns();
        configureFilters();
        selectCurrentMonth();
        applyFilter();
        loadCategories();
    }

    @FXML
    private void onFilter() {
        if (commitTypedDate(startPicker) && commitTypedDate(endPicker)) {
            applyFilter();
        } else {
            errorLabel.setText("Data inválida. Use o formato dd/mm/aaaa.");
            errorLabel.setVisible(true);
        }
    }

    /**
     * O campo só confirma o texto digitado ao perder o foco, e o Enter aciona o
     * botão Filtrar sem tirar o foco dele. Sem isto, o filtro usaria a data antiga.
     */
    private static boolean commitTypedDate(DatePicker picker) {
        String text = picker.getEditor().getText();
        LocalDate typed = picker.getConverter().fromString(text);
        if (typed == null && text != null && !text.isBlank()) {
            return false;
        }
        picker.setValue(typed);
        return true;
    }

    @FXML
    private void onClearFilters() {
        startPicker.setValue(null);
        endPicker.setValue(null);
        typeBox.getSelectionModel().selectFirst();
        categoryBox.getSelectionModel().selectFirst();
        applyFilter();
    }

    @FXML
    private void onPreviousPage() {
        load(currentPage - 1);
    }

    @FXML
    private void onNextPage() {
        load(currentPage + 1);
    }

    private void configureColumns() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        bindText(dateColumn, item -> Formats.date(item.data()));
        bindText(descriptionColumn, item -> item.descricao() == null ? "" : item.descricao());
        bindText(categoryColumn, item -> item.categoria().nome());
        bindText(typeColumn, item -> item.tipo().label());
        amountColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        amountColumn.setCellFactory(column -> new AmountCell());
    }

    private static void bindText(
            TableColumn<LancamentoResponse, String> column, Function<LancamentoResponse, String> text) {
        column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(text.apply(row.getValue())));
    }

    private void configureFilters() {
        typeBox.getItems().add(new Option<>("Todos", null));
        for (Tipo tipo : Tipo.values()) {
            typeBox.getItems().add(new Option<>(tipo.label(), tipo));
        }
        typeBox.getSelectionModel().selectFirst();
        // Com um tipo escolhido, só faz sentido oferecer as categorias desse tipo.
        typeBox.valueProperty().addListener((property, previous, selected) -> refreshCategoryOptions());
        refreshCategoryOptions();
    }

    private void selectCurrentMonth() {
        YearMonth month = YearMonth.now();
        startPicker.setValue(month.atDay(1));
        endPicker.setValue(month.atEndOfMonth());
    }

    private void loadCategories() {
        // As desativadas entram porque ainda podem ter lançamentos para filtrar.
        tasks.run(api::listCategorias, loaded -> {
            categories = loaded;
            refreshCategoryOptions();
        }, this::showError);
    }

    private void refreshCategoryOptions() {
        Long selected = categoryBox.getValue() == null ? null : categoryBox.getValue().value();
        Tipo tipo = typeBox.getValue() == null ? null : typeBox.getValue().value();

        List<Option<Long>> options = new ArrayList<>();
        options.add(new Option<>("Todas", null));
        for (CategoriaResponse category : categories) {
            if (tipo == null || category.tipo() == tipo) {
                options.add(new Option<>(categoryLabel(category), category.id()));
            }
        }
        categoryBox.getItems().setAll(options);
        categoryBox.setValue(options.stream()
                .filter(option -> Objects.equals(option.value(), selected))
                .findFirst()
                .orElse(options.getFirst()));
    }

    private static String categoryLabel(CategoriaResponse category) {
        return category.ativa() ? category.nome() : category.nome() + " (inativa)";
    }

    private void applyFilter() {
        LocalDate start = startPicker.getValue();
        LocalDate end = endPicker.getValue();
        appliedFilter = new LancamentoFilter(start, end, typeBox.getValue().value(), categoryBox.getValue().value());
        load(0);
    }

    private void load(int page) {
        // Uma resposta lenta de um filtro antigo não pode sobrescrever a do filtro atual.
        long request = ++lastRequest;
        LancamentoFilter filter = appliedFilter;
        previousButton.setDisable(true);
        nextButton.setDisable(true);
        errorLabel.setVisible(false);
        tasks.run(() -> api.listLancamentos(filter, page, PAGE_SIZE),
                result -> {
                    if (request == lastRequest) {
                        showPage(result);
                    }
                },
                failure -> {
                    if (request == lastRequest) {
                        // Manter as linhas antigas faria elas parecerem o resultado do filtro novo.
                        table.getItems().clear();
                        placeholderLabel.setText("Não foi possível carregar os lançamentos.");
                        pageLabel.setText("");
                        showError(failure);
                    }
                });
    }

    private void showPage(PageResponse<LancamentoResponse> result) {
        currentPage = result.pagina();
        placeholderLabel.setText("Nenhum lançamento encontrado.");
        table.getItems().setAll(result.conteudo());
        table.scrollTo(0);
        previousButton.setDisable(currentPage <= 0);
        nextButton.setDisable(currentPage >= result.totalPaginas() - 1);
        pageLabel.setText(result.totalPaginas() == 0
                ? ""
                : "Página %d de %d · %d %s".formatted(
                        currentPage + 1, result.totalPaginas(), result.totalElementos(),
                        result.totalElementos() == 1 ? "lançamento" : "lançamentos"));
    }

    private void showError(ApiException failure) {
        errorLabel.setText(failure.describe(Map.of()));
        errorLabel.setVisible(true);
    }

    /**
     * Item de um filtro. O {@code toString} é o texto que o {@link ComboBox} exibe.
     */
    private record Option<T>(String label, T value) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** Valor em reais, à direita, na cor do tipo do lançamento. */
    private static final class AmountCell extends TableCell<LancamentoResponse, LancamentoResponse> {
        @Override
        protected void updateItem(LancamentoResponse item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll(REVENUE_STYLE, EXPENSE_STYLE);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(Formats.currency(item.valor()));
                getStyleClass().add(item.tipo() == Tipo.RECEITA ? REVENUE_STYLE : EXPENSE_STYLE);
            }
        }
    }
}
