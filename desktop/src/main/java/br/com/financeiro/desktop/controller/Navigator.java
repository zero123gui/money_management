package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.dto.CategoriaResponse;
import br.com.financeiro.desktop.api.dto.LancamentoResponse;
import br.com.financeiro.desktop.session.Session;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
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
    private Stage openForm;

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

    /** Encerra a sessão de uma conta que acabou de ser excluída e avisa no login. */
    public void onAccountDeleted() {
        session.clear();
        showLoginWithNotice("Sua conta e todos os seus dados foram excluídos.");
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

    /**
     * Abre o formulário de lançamento em uma janela sobre a principal.
     *
     * @param entry   lançamento a editar, ou {@code null} para criar um novo
     * @param onSaved recebe o lançamento depois de salvo, com o formulário já fechado
     */
    public void openEntryForm(LancamentoResponse entry, Consumer<LancamentoResponse> onSaved) {
        this.<LancamentoFormController>openForm("lancamento-form.fxml",
                entry == null ? "Novo lançamento" : "Editar lançamento",
                (controller, form) -> controller.open(entry, saved -> {
                    form.close();
                    onSaved.accept(saved);
                }, form::close));
    }

    /**
     * Abre o formulário de categoria em uma janela sobre a principal.
     *
     * @param category categoria a renomear, ou {@code null} para criar uma nova
     * @param onSaved  recebe a categoria depois de salva, com o formulário já fechado
     */
    public void openCategoryForm(CategoriaResponse category, Consumer<CategoriaResponse> onSaved) {
        this.<CategoriaFormController>openForm("categoria-form.fxml",
                category == null ? "Nova categoria" : "Renomear categoria",
                (controller, form) -> controller.open(category, saved -> {
                    form.close();
                    onSaved.accept(saved);
                }, form::close));
    }

    private <C> void openForm(String view, String title, BiConsumer<C, Stage> prepare) {
        FXMLLoader loader = newLoader(view);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível abrir a tela " + view, e);
        }
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(stage.getScene().getStylesheets());

        Stage form = new Stage();
        form.initOwner(stage);
        form.initModality(Modality.WINDOW_MODAL);
        form.setTitle(title);
        form.setResizable(false);
        form.setScene(scene);
        openForm = form;

        prepare.accept(loader.getController(), form);
        form.show();
    }

    private void onSessionExpired() {
        // Várias chamadas podem expirar juntas; só a primeira leva ao login, senão
        // as seguintes apagariam o que o usuário já começou a digitar nele.
        if (session.isActive()) {
            // Sem isto, o formulário ficaria aberto e bloqueando a tela de login atrás dele.
            if (openForm != null) {
                openForm.close();
            }
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
        if (type == ResumoController.class) {
            return new ResumoController(api, tasks);
        }
        if (type == ContaController.class) {
            return new ContaController(api, tasks, this);
        }
        if (type == ExcluidosController.class) {
            return new ExcluidosController(api, tasks);
        }
        if (type == CategoriasController.class) {
            return new CategoriasController(api, tasks, this);
        }
        if (type == CategoriaFormController.class) {
            return new CategoriaFormController(api, tasks);
        }
        if (type == LancamentosController.class) {
            return new LancamentosController(api, tasks, this);
        }
        if (type == LancamentoFormController.class) {
            return new LancamentoFormController(api, tasks);
        }
        throw new IllegalArgumentException("Controller desconhecido: " + type.getName());
    }
}
