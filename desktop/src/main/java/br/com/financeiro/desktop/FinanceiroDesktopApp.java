package br.com.financeiro.desktop;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiConfig;
import br.com.financeiro.desktop.controller.Navigator;
import br.com.financeiro.desktop.session.Session;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final long CLOSE_LOGOUT_WAIT_SECONDS = 5;
    private static final Logger LOGGER = Logger.getLogger(FinanceiroDesktopApp.class.getName());

    private Session session;
    private ApiClient api;

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
        session = new Session();
        api = new ApiClient(config, session);
        Navigator navigator = new Navigator(stage, session, api);

        stage.setTitle("Controle Financeiro");
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        navigator.showLogin();
        stage.show();
    }

    /**
     * Ao fechar o aplicativo, encerra o token na API, como no botão Sair. Espera
     * pouco: se a API demorar, o token vence sozinho em até 1 hora.
     */
    @Override
    public void stop() {
        String token = session == null ? null : session.token();
        if (token == null) {
            return;
        }
        session.clear();
        try {
            CompletableFuture.runAsync(() -> api.logout(token)).get(CLOSE_LOGOUT_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.log(Level.INFO, "Não foi possível encerrar o token ao fechar o aplicativo", e);
        }
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
