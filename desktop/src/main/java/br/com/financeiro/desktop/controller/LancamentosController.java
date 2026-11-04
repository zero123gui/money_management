package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiException;
import br.com.financeiro.desktop.api.LancamentoFilter;
import br.com.financeiro.desktop.api.dto.CategoriaResponse;
import br.com.financeiro.desktop.api.dto.LancamentoResponse;
import br.com.financeiro.desktop.api.dto.PageResponse;
import br.com.financeiro.desktop.api.dto.Tipo;
import br.com.financeiro.desktop.format.Formats;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/**
 * Lista de lançamentos, com filtros e paginação (RF-10), e as ações de criar,
 * editar e excluir (RF-06 a RF-09). Abre no mês atual.
 */
public class LancamentosController {

    private static final int PAGE_SIZE = 50;
    private static final String REVENUE_STYLE = "amount-revenue";
    private static final String EXPENSE_STYLE = "amount-expense";

    private final ApiClient api;
    private final ApiTasks tasks;
    private final Navigator navigator;

    @FXML
    private DatePicker startPicker;
    @FXML
    private DatePicker endPicker;
    @FXML
    private ComboBox<Option<Tipo>> typeBox;
    @FXML
    private ComboBox<Option<Long>> categoryBox;
    @FXML
    private Label noticeLabel;
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
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;

    private List<CategoriaResponse> categories = List.of();
    private LancamentoFilter appliedFilter;
    private int currentPage;
    private long lastRequest;

    /**
     * Cria o controller.
     *
     * @param api       cliente da API
     * @param tasks     executor das chamadas
     * @param navigator abre o formulário
     */
    public LancamentosController(ApiClient api, ApiTasks tasks, Navigator navigator) {
        this.api = api;
        this.tasks = tasks;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        noticeLabel.managedProperty().bind(noticeLabel.visibleProperty());
        startPicker.setConverter(Formats.dateConverter());
        endPicker.setConverter(Formats.dateConverter());
        configureColumns();
        configureRows();
        configureFilters();
        selectCurrentMonth();
        applyFilter();
        loadCategories();
    }

    @FXML
    private void onFilter() {
        noticeLabel.setVisible(false);
        if (DateFields.commitTyped(startPicker) && DateFields.commitTyped(endPicker)) {
            applyFilter();
        } else {
            errorLabel.setText("Data inválida. Use o formato dd/mm/aaaa.");
            errorLabel.setVisible(true);
        }
    }

    @FXML
    private void onClearFilters() {
        noticeLabel.setVisible(false);
        startPicker.setValue(null);
        endPicker.setValue(null);
        typeBox.getSelectionModel().selectFirst();
        categoryBox.getSelectionModel().selectFirst();
        applyFilter();
    }

    @FXML
    private void onPreviousPage() {
        noticeLabel.setVisible(false);
        load(currentPage - 1);
    }

    @FXML
    private void onNextPage() {
        noticeLabel.setVisible(false);
        load(currentPage + 1);
    }

    @FXML
    private void onNew() {
        navigator.openEntryForm(null, saved -> onSaved(saved, "Lançamento criado."));
    }

    @FXML
    private void onEdit() {
        edit(table.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onDelete() {
        LancamentoResponse entry = table.getSelectionModel().getSelectedItem();
        if (entry != null && confirmDelete(entry)) {
            tasks.run(() -> {
                api.deleteLancamento(entry.id());
                return entry;
            }, deleted -> {
                load(currentPage);
                showNotice("Lançamento excluído.");
            }, failure -> {
                // A lista pode estar velha: quem falhou talvez já tenha sido excluído em outro aparelho.
                load(currentPage);
                showError(failure);
            });
        }
    }

    private void edit(LancamentoResponse entry) {
        if (entry != null) {
            navigator.openEntryForm(entry, saved -> onSaved(saved, "Lançamento salvo."));
        }
    }

    private void onSaved(LancamentoResponse saved, String message) {
        load(currentPage);
        showNotice(appliedFilter.matches(saved)
                ? message
                : message + " Ele não aparece na lista por causa dos filtros atuais.");
    }

    private boolean confirmDelete(LancamentoResponse entry) {
        ButtonType delete = new ButtonType("Excluir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", delete, cancel);
        alert.initOwner(table.getScene().getWindow());
        alert.setTitle("Excluir lançamento");
        alert.setHeaderText("Excluir este lançamento?");
        alert.setContentText("%s, %s%s%n%nEle deixa de contar no saldo e pode ser restaurado depois.".formatted(
                Formats.date(entry.data()), Formats.currency(entry.valor()),
                entry.descricao() == null ? "" : ", " + entry.descricao()));
        return alert.showAndWait().filter(delete::equals).isPresent();
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

    private void configureRows() {
        BooleanBinding nothingSelected = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(nothingSelected);
        deleteButton.disableProperty().bind(nothingSelected);
        table.setRowFactory(view -> {
            TableRow<LancamentoResponse> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    edit(row.getItem());
                }
            });
            return row;
        });
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
        appliedFilter = new LancamentoFilter(
                startPicker.getValue(), endPicker.getValue(),
                typeBox.getValue().value(), categoryBox.getValue().value());
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
                        noticeLabel.setVisible(false);
                        showError(failure);
                    }
                });
    }

    private void showPage(PageResponse<LancamentoResponse> result) {
        // Excluir o único item da última página deixaria o usuário em uma página que não existe mais.
        if (result.conteudo().isEmpty() && result.pagina() > 0) {
            load(Math.max(result.totalPaginas() - 1, 0));
            return;
        }
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

    private void showNotice(String text) {
        noticeLabel.setText(text);
        noticeLabel.setVisible(true);
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
