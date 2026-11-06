package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiException;
import br.com.financeiro.desktop.api.dto.LoginRequest;
import br.com.financeiro.desktop.session.Session;
import java.util.List;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * Tela de login (RF-02).
 */
public class LoginController {

    private static final Map<String, String> LABELS = Map.of("email", "E-mail", "senha", "Senha");
    private static final String ERROR_STYLE = "form-error";
    private static final String NOTICE_STYLE = "form-notice";
    private static final String SLOW_SERVER_MESSAGE =
            "Conectando ao servidor. O primeiro acesso do dia pode levar alguns minutos.";

    // A API dorme quando ninguém usa; sem o aviso, a espera pareceria um travamento.
    private final PauseTransition slowServerNotice = new PauseTransition(Duration.seconds(5));

    private final ApiClient api;
    private final Session session;
    private final ApiTasks tasks;
    private final Navigator navigator;

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;
    @FXML
    private Button loginButton;

    /**
     * Cria o controller.
     *
     * @param api       cliente da API
     * @param session   sessão aberta após o login
     * @param tasks     executor das chamadas
     * @param navigator troca de telas
     */
    public LoginController(ApiClient api, Session session, ApiTasks tasks, Navigator navigator) {
        this.api = api;
        this.session = session;
        this.tasks = tasks;
        this.navigator = navigator;
    }

    /**
     * Mostra um aviso que não é erro desta tela, como a sessão expirada.
     *
     * @param notice texto do aviso
     */
    public void showNotice(String notice) {
        showMessage(notice, NOTICE_STYLE);
    }

    @FXML
    private void initialize() {
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());
        slowServerNotice.setOnFinished(event -> showNotice(SLOW_SERVER_MESSAGE));
    }

    @FXML
    private void onLogin() {
        LoginRequest request = new LoginRequest(emailField.getText().strip(), passwordField.getText());
        loginButton.setDisable(true);
        messageLabel.setVisible(false);
        slowServerNotice.playFromStart();
        tasks.run(() -> api.login(request), token -> {
            slowServerNotice.stop();
            session.open(token.token());
            navigator.showMain();
        }, this::onLoginFailed);
    }

    @FXML
    private void onCreateAccount() {
        navigator.showRegister();
    }

    private void onLoginFailed(ApiException failure) {
        slowServerNotice.stop();
        passwordField.clear();
        loginButton.setDisable(false);
        showMessage(failure.describe(LABELS), ERROR_STYLE);
    }

    private void showMessage(String text, String style) {
        messageLabel.getStyleClass().removeAll(List.of(ERROR_STYLE, NOTICE_STYLE));
        messageLabel.getStyleClass().add(style);
        messageLabel.setText(text);
        messageLabel.setVisible(true);
    }
}
