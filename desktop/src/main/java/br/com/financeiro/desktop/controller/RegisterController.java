package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiException;
import br.com.financeiro.desktop.api.dto.CadastroRequest;
import br.com.financeiro.desktop.api.dto.LoginRequest;
import br.com.financeiro.desktop.session.Session;
import java.util.Map;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Tela de cadastro (RF-01). Depois de criar a conta, já entra com ela.
 */
public class RegisterController {

    private static final Map<String, String> LABELS = Map.of("nome", "Nome", "email", "E-mail", "senha", "Senha");

    private final ApiClient api;
    private final Session session;
    private final ApiTasks tasks;
    private final Navigator navigator;

    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmationField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button registerButton;

    /**
     * Cria o controller.
     *
     * @param api       cliente da API
     * @param session   sessão aberta após o cadastro
     * @param tasks     executor das chamadas
     * @param navigator troca de telas
     */
    public RegisterController(ApiClient api, Session session, ApiTasks tasks, Navigator navigator) {
        this.api = api;
        this.session = session;
        this.tasks = tasks;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    }

    @FXML
    private void onRegister() {
        // Só esta regra fica no cliente: a API recebe uma senha só e não tem como compará-las.
        if (!passwordField.getText().equals(confirmationField.getText())) {
            showError("As senhas não são iguais.");
            return;
        }
        CadastroRequest request = new CadastroRequest(
                nameField.getText().strip(), emailField.getText().strip(), passwordField.getText());
        registerButton.setDisable(true);
        errorLabel.setVisible(false);
        tasks.run(() -> registerAndLogin(request), this::onRegistered, failure -> {
            registerButton.setDisable(false);
            showError(failure.describe(LABELS));
        });
    }

    @FXML
    private void onBackToLogin() {
        navigator.showLogin();
    }

    /**
     * Uma falha no login não pode virar erro de cadastro: a conta já existe, e
     * tentar cadastrar de novo só resultaria em e-mail duplicado.
     */
    private Optional<String> registerAndLogin(CadastroRequest request) {
        api.register(request);
        try {
            return Optional.of(api.login(new LoginRequest(request.email(), request.senha())).token());
        } catch (ApiException e) {
            return Optional.empty();
        }
    }

    private void onRegistered(Optional<String> token) {
        if (token.isPresent()) {
            session.open(token.get());
            navigator.showMain();
        } else {
            navigator.showLoginWithNotice("Conta criada. Entre com seu e-mail e senha.");
        }
    }

    private void showError(String text) {
        errorLabel.setText(text);
        errorLabel.setVisible(true);
    }
}
