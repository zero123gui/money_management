package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Tela principal. Por enquanto só identifica o usuário e oferece o logout; o
 * menu e as demais telas entram nas próximas etapas.
 */
public class MainController {

    private final ApiClient api;
    private final ApiTasks tasks;
    private final Navigator navigator;

    @FXML
    private Label userLabel;
    @FXML
    private Label contentLabel;

    /**
     * Cria o controller.
     *
     * @param api       cliente da API
     * @param tasks     executor das chamadas
     * @param navigator troca de telas
     */
    public MainController(ApiClient api, ApiTasks tasks, Navigator navigator) {
        this.api = api;
        this.tasks = tasks;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        tasks.run(api::currentUser,
                user -> {
                    userLabel.setText(user.nome());
                    contentLabel.setText("Olá, " + user.nome() + "!");
                },
                failure -> contentLabel.setText(failure.describe(Map.of())));
    }

    @FXML
    private void onLogout() {
        navigator.logout();
    }
}
