package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.dto.CategoriaResponse;
import br.com.financeiro.desktop.api.dto.LancamentoRequest;
import br.com.financeiro.desktop.api.dto.LancamentoResponse;
import br.com.financeiro.desktop.api.dto.Tipo;
import br.com.financeiro.desktop.format.Formats;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * Formulário para criar ou editar um lançamento (RF-06 a RF-08). As regras dos
 * campos ficam na API; aqui só se converte o que ela não teria como ler.
 */
public class LancamentoFormController {

    private static final Map<String, String> LABELS = Map.of(
            "tipo", "Tipo", "valor", "Valor", "data", "Data", "descricao", "Descrição", "categoriaId", "Categoria");

    private final ApiClient api;
    private final ApiTasks tasks;

    @FXML
    private Label titleLabel;
    @FXML
    private ComboBox<Tipo> typeBox;
    @FXML
    private TextField amountField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<CategoriaResponse> categoryBox;
    @FXML
    private TextField descriptionField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    private List<CategoriaResponse> categories = List.of();
    private LancamentoResponse entry;
    private Consumer<LancamentoResponse> onSaved;
    private Runnable onCancel;

    /**
     * Cria o controller.
     *
     * @param api   cliente da API
     * @param tasks executor das chamadas
     */
    public LancamentoFormController(ApiClient api, ApiTasks tasks) {
        this.api = api;
        this.tasks = tasks;
    }

    /**
     * Prepara o formulário e busca as categorias.
     *
     * @param entry    lançamento a editar, ou {@code null} para criar um novo
     * @param onSaved  recebe o lançamento depois de salvo
     * @param onCancel executado ao cancelar
     */
    public void open(LancamentoResponse entry, Consumer<LancamentoResponse> onSaved, Runnable onCancel) {
        this.entry = entry;
        this.onSaved = onSaved;
        this.onCancel = onCancel;
        if (entry == null) {
            titleLabel.setText("Novo lançamento");
            typeBox.setValue(Tipo.DESPESA);
            datePicker.setValue(LocalDate.now());
        } else {
            titleLabel.setText("Editar lançamento");
            typeBox.setValue(entry.tipo());
            amountField.setText(Formats.amount(entry.valor()));
            datePicker.setValue(entry.data());
            descriptionField.setText(entry.descricao());
        }
        // Salvar só é liberado com as categorias na tela.
        saveButton.setDisable(true);
        tasks.run(api::listCategorias, loaded -> {
            categories = loaded;
            refreshCategoryOptions();
            saveButton.setDisable(false);
        }, failure -> showError(failure.describe(LABELS)));
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        datePicker.setConverter(Formats.dateConverter());
        typeBox.getItems().setAll(Tipo.values());
        typeBox.setConverter(textConverter(Tipo::label));
        categoryBox.setConverter(textConverter(
                category -> category.ativa() ? category.nome() : category.nome() + " (inativa)"));
        typeBox.valueProperty().addListener((property, previous, selected) -> refreshCategoryOptions());
    }

    @FXML
    private void onSave() {
        BigDecimal amount = null;
        if (!amountField.getText().isBlank()) {
            amount = Formats.parseAmount(amountField.getText());
            if (amount == null) {
                showError("Valor inválido. Use o formato 1.234,56.");
                return;
            }
        }
        if (!DateFields.commitTyped(datePicker)) {
            showError("Data inválida. Use o formato dd/mm/aaaa.");
            return;
        }
        CategoriaResponse category = categoryBox.getValue();
        LancamentoRequest request = new LancamentoRequest(
                typeBox.getValue(), amount, datePicker.getValue(), descriptionField.getText(),
                category == null ? null : category.id());

        saveButton.setDisable(true);
        errorLabel.setVisible(false);
        tasks.run(() -> entry == null ? api.createLancamento(request) : api.updateLancamento(entry.id(), request),
                onSaved,
                failure -> {
                    saveButton.setDisable(false);
                    showError(failure.describe(LABELS));
                });
    }

    @FXML
    private void onCancel() {
        onCancel.run();
    }

    /**
     * Oferece só as categorias ativas do tipo escolhido, mais a atual do
     * lançamento, para ele não abrir sem categoria se ela tiver sido desativada.
     */
    private void refreshCategoryOptions() {
        Long currentId = categoryBox.getValue() != null ? categoryBox.getValue().id()
                : entry != null ? entry.categoria().id() : null;
        List<CategoriaResponse> options = categories.stream()
                .filter(category -> category.tipo() == typeBox.getValue())
                .filter(category -> category.ativa() || category.id().equals(currentId))
                .toList();
        categoryBox.getItems().setAll(options);
        categoryBox.setValue(options.stream()
                .filter(category -> category.id().equals(currentId))
                .findFirst()
                .orElse(null));
    }

    private void showError(String text) {
        errorLabel.setText(text);
        errorLabel.setVisible(true);
        // A janela não é redimensionável: sem isto, a mensagem cortaria e empurraria os botões para fora.
        errorLabel.getScene().getWindow().sizeToScene();
    }

    private static <T> StringConverter<T> textConverter(Function<T, String> text) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : text.apply(value);
            }

            @Override
            public T fromString(String value) {
                throw new UnsupportedOperationException("Este campo não aceita digitação");
            }
        };
    }
}
