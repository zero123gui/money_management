package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.dto.CategoriaCreateRequest;
import br.com.financeiro.desktop.api.dto.CategoriaResponse;
import br.com.financeiro.desktop.api.dto.CategoriaUpdateRequest;
import br.com.financeiro.desktop.api.dto.Tipo;
import java.util.Map;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * Formulário para criar ou renomear uma categoria própria (RF-15). As regras do
 * nome ficam na API.
 */
public class CategoriaFormController {

    private static final Map<String, String> LABELS = Map.of("nome", "Nome", "tipo", "Tipo");

    private final ApiClient api;
    private final ApiTasks tasks;

    @FXML
    private Label titleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<Tipo> typeBox;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    private CategoriaResponse category;
    private Consumer<CategoriaResponse> onSaved;
    private Runnable onCancel;

    /**
     * Cria o controller.
     *
     * @param api   cliente da API
     * @param tasks executor das chamadas
     */
    public CategoriaFormController(ApiClient api, ApiTasks tasks) {
        this.api = api;
        this.tasks = tasks;
    }

    /**
     * Prepara o formulário.
     *
     * @param category categoria a renomear, ou {@code null} para criar uma nova
     * @param onSaved  recebe a categoria depois de salva
     * @param onCancel executado ao cancelar
     */
    public void open(CategoriaResponse category, Consumer<CategoriaResponse> onSaved, Runnable onCancel) {
        this.category = category;
        this.onSaved = onSaved;
        this.onCancel = onCancel;
        if (category == null) {
            titleLabel.setText("Nova categoria");
            typeBox.setValue(Tipo.DESPESA);
        } else {
            titleLabel.setText("Renomear categoria");
            nameField.setText(category.nome());
            typeBox.setValue(category.tipo());
            // O tipo não muda depois de criado: deixaria os lançamentos antigos incoerentes.
            typeBox.setDisable(true);
        }
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        typeBox.getItems().setAll(Tipo.values());
        typeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Tipo value) {
                return value == null ? "" : value.label();
            }

            @Override
            public Tipo fromString(String value) {
                throw new UnsupportedOperationException("Este campo não aceita digitação");
            }
        });
    }

    @FXML
    private void onSave() {
        String name = nameField.getText();
        Tipo type = typeBox.getValue();
        saveButton.setDisable(true);
        errorLabel.setVisible(false);
        tasks.run(() -> category == null
                        ? api.createCategoria(new CategoriaCreateRequest(name, type))
                        : api.updateCategoria(category.id(), new CategoriaUpdateRequest(name, category.ativa())),
                onSaved,
                failure -> {
                    saveButton.setDisable(false);
                    errorLabel.setText(failure.describe(LABELS));
                    errorLabel.setVisible(true);
                    // A janela não é redimensionável: sem isto, a mensagem cortaria e empurraria os botões para fora.
                    errorLabel.getScene().getWindow().sizeToScene();
                });
    }

    @FXML
    private void onCancel() {
        onCancel.run();
    }
}
