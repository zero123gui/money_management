package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

/**
 * Tela principal: identifica o usuário, oferece o logout e troca o conteúdo
 * pelo menu lateral. Cada item do menu guarda o FXML da sua tela em {@code userData}.
 */
public class MainController {

    private final ApiClient api;
    private final ApiTasks tasks;
    private final Navigator navigator;

    @FXML
    private Label userLabel;
    @FXML
    private ToggleGroup menu;
    @FXML
    private ToggleButton resumoItem;
    @FXML
    private StackPane contentPane;

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
        menu.selectedToggleProperty().addListener((property, previous, selected) -> {
            if (selected == null) {
                // Clicar no item já selecionado o desmarcaria, deixando o menu sem seleção.
                previous.setSelected(true);
            } else {
                contentPane.getChildren().setAll(navigator.loadContent((String) selected.getUserData()));
            }
        });
        resumoItem.setSelected(true);

        // Sem o nome o aplicativo continua útil, e a tela aberta já mostra a falha da API.
        tasks.run(api::currentUser, user -> userLabel.setText(user.nome()), failure -> userLabel.setText(""));
    }

    @FXML
    private void onLogout() {
        navigator.logout();
    }
}
