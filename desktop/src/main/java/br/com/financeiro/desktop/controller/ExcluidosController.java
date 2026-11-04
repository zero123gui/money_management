package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiException;
import br.com.financeiro.desktop.api.dto.LancamentoExcluidoResponse;
import br.com.financeiro.desktop.api.dto.PageResponse;
import br.com.financeiro.desktop.format.Formats;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Lista os lançamentos excluídos e permite restaurá-los (RF-24).
 */
public class ExcluidosController {

    private static final int PAGE_SIZE = 50;

    private final ApiClient api;
    private final ApiTasks tasks;

    @FXML
    private Label noticeLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private TableView<LancamentoExcluidoResponse> table;
    @FXML
    private Label placeholderLabel;
    @FXML
    private TableColumn<LancamentoExcluidoResponse, String> deletedAtColumn;
    @FXML
    private TableColumn<LancamentoExcluidoResponse, String> dateColumn;
    @FXML
    private TableColumn<LancamentoExcluidoResponse, String> descriptionColumn;
    @FXML
    private TableColumn<LancamentoExcluidoResponse, String> categoryColumn;
    @FXML
    private TableColumn<LancamentoExcluidoResponse, String> typeColumn;
    @FXML
    private TableColumn<LancamentoExcluidoResponse, LancamentoExcluidoResponse> amountColumn;
    @FXML
    private Label pageLabel;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button restoreButton;

    private int currentPage;
    private long lastRequest;

    /**
     * Cria o controller.
     *
     * @param api   cliente da API
     * @param tasks executor das chamadas
     */
    public ExcluidosController(ApiClient api, ApiTasks tasks) {
        this.api = api;
        this.tasks = tasks;
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        noticeLabel.managedProperty().bind(noticeLabel.visibleProperty());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        bindText(deletedAtColumn, item -> Formats.dateTime(item.excluidoEm()));
        bindText(dateColumn, item -> Formats.date(item.data()));
        bindText(descriptionColumn, item -> item.descricao() == null ? "" : item.descricao());
        bindText(categoryColumn, item -> item.categoria().nome());
        bindText(typeColumn, item -> item.tipo().label());
        amountColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        amountColumn.setCellFactory(column ->
                new AmountCell<>(LancamentoExcluidoResponse::valor, LancamentoExcluidoResponse::tipo));
        restoreButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        load(0);
    }

    @FXML
    private void onRestore() {
        LancamentoExcluidoResponse entry = table.getSelectionModel().getSelectedItem();
        if (entry == null) {
            return;
        }
        tasks.run(() -> api.restoreLancamento(entry.id()), restored -> {
            load(currentPage);
            noticeLabel.setText("Lançamento restaurado. Ele voltou a contar no saldo e nos resumos.");
            noticeLabel.setVisible(true);
        }, failure -> {
            // A lista pode estar velha: ele pode já ter sido restaurado em outro aparelho.
            load(currentPage);
            showError(failure);
        });
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

    private static void bindText(
            TableColumn<LancamentoExcluidoResponse, String> column,
            Function<LancamentoExcluidoResponse, String> text) {
        column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(text.apply(row.getValue())));
    }

    private void load(int page) {
        // Uma resposta lenta não pode sobrescrever a de uma ação mais recente.
        long request = ++lastRequest;
        previousButton.setDisable(true);
        nextButton.setDisable(true);
        errorLabel.setVisible(false);
        tasks.run(() -> api.listLancamentosExcluidos(page, PAGE_SIZE),
                result -> {
                    if (request == lastRequest) {
                        showPage(result);
                    }
                },
                failure -> {
                    if (request == lastRequest) {
                        table.getItems().clear();
                        placeholderLabel.setText("Não foi possível carregar os lançamentos excluídos.");
                        pageLabel.setText("");
                        noticeLabel.setVisible(false);
                        showError(failure);
                    }
                });
    }

    private void showPage(PageResponse<LancamentoExcluidoResponse> result) {
        // Restaurar o único item da última página deixaria o usuário em uma página que não existe mais.
        if (result.conteudo().isEmpty() && result.pagina() > 0) {
            load(Math.max(result.totalPaginas() - 1, 0));
            return;
        }
        currentPage = result.pagina();
        placeholderLabel.setText("Nenhum lançamento excluído.");
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
}
