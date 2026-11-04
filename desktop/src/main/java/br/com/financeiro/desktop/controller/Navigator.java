package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.session.Session;
import java.io.IOException;
import java.io.UncheckedIOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Troca as telas da janela principal e entrega a cada controller o que ele usa.
 */
public class Navigator {

    private static final String RESOURCES = "/br/com/financeiro/desktop/";
    private static final double WIDTH = 960;
    private static final double HEIGHT = 640;

    private final Stage stage;
    private final Session session;
    private final ApiClient api;
    private final ApiTasks tasks = new ApiTasks(this::onSessionExpired);

    /**
     * Cria o navegador.
     *
     * @param stage   janela principal
     * @param session sessão do usuário
     * @param api     cliente da API
     */
    public Navigator(Stage stage, Session session, ApiClient api) {
        this.stage = stage;
        this.session = session;
        this.api = api;
    }

    /** Mostra a tela de login. */
    public void showLogin() {
        load("login.fxml");
    }

    /** Mostra a tela de cadastro. */
    public void showRegister() {
        load("register.fxml");
    }

    /** Mostra a tela principal, para o usuário logado. */
    public void showMain() {
        load("main.fxml");
    }

    /** Encerra a sessão e volta para o login (RF-03). */
    public void logout() {
        session.clear();
        showLogin();
    }

    /**
     * Mostra o login com um aviso.
     *
     * @param notice texto do aviso
     */
    public void showLoginWithNotice(String notice) {
        LoginController controller = load("login.fxml");
        controller.showNotice(notice);
    }

    private void onSessionExpired() {
        // Várias chamadas podem expirar juntas; só a primeira leva ao login, senão
        // as seguintes apagariam o que o usuário já começou a digitar nele.
        if (session.isActive()) {
            session.clear();
            showLoginWithNotice("Sua sessão expirou. Entre novamente.");
        }
    }

    /**
     * Carrega uma tela para ser exibida dentro da tela principal.
     *
     * @param view nome do arquivo FXML
     * @return raiz da tela
     */
    public Parent loadContent(String view) {
        FXMLLoader loader = newLoader(view);
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível abrir a tela " + view, e);
        }
    }

    private FXMLLoader newLoader(String view) {
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(RESOURCES + "view/" + view));
        loader.setControllerFactory(this::createController);
        return loader;
    }

    private <T> T load(String view) {
        FXMLLoader loader = newLoader(view);
        try {
            Parent root = loader.load();
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, WIDTH, HEIGHT);
                scene.getStylesheets().add(Navigator.class.getResource(RESOURCES + "css/app.css").toExternalForm());
                stage.setScene(scene);
            } else {
                stage.getScene().setRoot(root);
            }
            return loader.getController();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível abrir a tela " + view, e);
        }
    }

    private Object createController(Class<?> type) {
        if (type == LoginController.class) {
            return new LoginController(api, session, tasks, this);
        }
        if (type == RegisterController.class) {
            return new RegisterController(api, session, tasks, this);
        }
        if (type == MainController.class) {
            return new MainController(api, tasks, this);
        }
        if (type == LancamentosController.class) {
            return new LancamentosController(api, tasks);
        }
        throw new IllegalArgumentException("Controller desconhecido: " + type.getName());
    }
}
