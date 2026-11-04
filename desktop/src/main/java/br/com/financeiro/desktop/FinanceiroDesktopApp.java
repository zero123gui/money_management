package br.com.financeiro.desktop;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiConfig;
import br.com.financeiro.desktop.controller.Navigator;
import br.com.financeiro.desktop.session.Session;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Ponto de entrada do cliente desktop.
 */
public class FinanceiroDesktopApp extends Application {

    private static final double MIN_WIDTH = 720;
    private static final double MIN_HEIGHT = 560;

    /**
     * Monta o aplicativo e abre a tela de login.
     *
     * @param stage janela principal
     */
    @Override
    public void start(Stage stage) {
        ApiConfig config;
        try {
            config = ApiConfig.fromEnvironment();
        } catch (IllegalArgumentException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }
        Session session = new Session();
        Navigator navigator = new Navigator(stage, session, new ApiClient(config, session));

        stage.setTitle("Controle Financeiro");
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        navigator.showLogin();
        stage.show();
    }

    /**
     * Inicia o JavaFX.
     *
     * @param args argumentos da linha de comando
     */
    public static void main(String[] args) {
        launch(args);
    }
}
