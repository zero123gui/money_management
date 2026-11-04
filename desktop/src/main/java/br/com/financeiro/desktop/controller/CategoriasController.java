package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiException;
import br.com.financeiro.desktop.api.dto.CategoriaResponse;
import br.com.financeiro.desktop.api.dto.CategoriaUpdateRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/**
 * Lista as categorias padrão e as próprias, e permite criar, renomear, desativar
 * e reativar as próprias (RF-14, RF-15). Os botões só evitam uma tentativa
 * inútil nas categorias padrão: quem proíbe é a API.
 */
public class CategoriasController {

    private final ApiClient api;
    private final ApiTasks tasks;
    private final Navigator navigator;

    @FXML
    private Label noticeLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private TableView<CategoriaResponse> table;
    @FXML
    private Label placeholderLabel;
    @FXML
    private TableColumn<CategoriaResponse, String> nameColumn;
    @FXML
    private TableColumn<CategoriaResponse, String> typeColumn;
    @FXML
    private TableColumn<CategoriaResponse, String> originColumn;
    @FXML
    private TableColumn<CategoriaResponse, String> statusColumn;
    @FXML
    private Button renameButton;
    @FXML
    private Button toggleButton;

    private long lastRequest;

    /**
     * Cria o controller.
     *
     * @param api       cliente da API
     * @param tasks     executor das chamadas
     * @param navigator abre o formulário
     */
    public CategoriasController(ApiClient api, ApiTasks tasks, Navigator navigator) {
        this.api = api;
        this.tasks = tasks;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        noticeLabel.managedProperty().bind(noticeLabel.visibleProperty());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        bindText(nameColumn, CategoriaResponse::nome);
        bindText(typeColumn, category -> category.tipo().label());
        bindText(originColumn, category -> category.padrao() ? "Padrão" : "Própria");
        bindText(statusColumn, category -> category.ativa() ? "Ativa" : "Inativa");
        table.getSelectionModel().selectedItemProperty()
                .addListener((property, previous, selected) -> refreshActions(selected));
        table.setRowFactory(view -> {
            TableRow<CategoriaResponse> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && !row.getItem().padrao()) {
                    rename(row.getItem());
                }
            });
            return row;
        });
        refreshActions(null);
        load();
    }

    @FXML
    private void onNew() {
        navigator.openCategoryForm(null, saved -> onChanged("Categoria criada."));
    }

    @FXML
    private void onRename() {
        rename(table.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onToggle() {
        CategoriaResponse category = table.getSelectionModel().getSelectedItem();
        if (category == null) {
            return;
        }
        boolean activate = !category.ativa();
        toggleButton.setDisable(true);
        tasks.run(() -> api.updateCategoria(category.id(), new CategoriaUpdateRequest(category.nome(), activate)),
                saved -> onChanged(activate
                        ? "Categoria reativada."
                        : "Categoria desativada. Ela não aparece mais para novos lançamentos, e o histórico continua igual."),
                failure -> {
                    // A lista pode estar velha: a categoria pode ter mudado em outro aparelho.
                    load();
                    showError(failure);
                });
    }

    private void rename(CategoriaResponse category) {
        if (category != null) {
            navigator.openCategoryForm(category, saved -> onChanged("Categoria renomeada."));
        }
    }

    private void onChanged(String message) {
        load();
        noticeLabel.setText(message);
        noticeLabel.setVisible(true);
    }

    private void refreshActions(CategoriaResponse selected) {
        boolean editable = selected != null && !selected.padrao();
        renameButton.setDisable(!editable);
        toggleButton.setDisable(!editable);
        toggleButton.setText(selected != null && !selected.ativa() ? "Reativar" : "Desativar");
    }

    private static void bindText(
            TableColumn<CategoriaResponse, String> column, Function<CategoriaResponse, String> text) {
        column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(text.apply(row.getValue())));
    }

    private void load() {
        // Uma resposta lenta não pode sobrescrever a de uma alteração mais recente.
        long request = ++lastRequest;
        errorLabel.setVisible(false);
        tasks.run(api::listCategorias,
                loaded -> {
                    if (request == lastRequest) {
                        show(loaded);
                    }
                },
                failure -> {
                    if (request == lastRequest) {
                        table.getItems().clear();
                        placeholderLabel.setText("Não foi possível carregar as categorias.");
                        noticeLabel.setVisible(false);
                        showError(failure);
                    }
                });
    }

    private void show(List<CategoriaResponse> categories) {
        placeholderLabel.setText("Nenhuma categoria encontrada.");
        table.getItems().setAll(categories);
        refreshActions(table.getSelectionModel().getSelectedItem());
    }

    private void showError(ApiException failure) {
        errorLabel.setText(failure.describe(Map.of("nome", "Nome")));
        errorLabel.setVisible(true);
    }
}
