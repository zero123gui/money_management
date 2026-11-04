package br.com.financeiro.desktop.controller;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiConfig;
import br.com.financeiro.desktop.session.Session;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Abre cada tela de verdade. Um {@code fx:id} ou {@code onAction} sem par no
 * controller só falha ao carregar o FXML, e o compilador não acusa.
 */
class ViewsLoadTest {

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        } catch (UnsupportedOperationException noDisplay) {
            Assumptions.abort("Ambiente sem tela: " + noDisplay.getMessage());
        }
        Platform.setImplicitExit(false);
        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
    }

    private static Parent onJavaFxThread(java.util.function.Consumer<Navigator> action) throws Exception {
        CompletableFuture<Parent> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                Session session = new Session();
                // A porta 1 não responde: a tela principal mostra o erro em vez de travar o teste.
                ApiClient api = new ApiClient(ApiConfig.of("http://127.0.0.1:1"), session);
                action.accept(new Navigator(stage, session, api));
                result.complete(stage.getScene().getRoot());
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    @Test
    void loginViewLoads() throws Exception {
        assertThat(onJavaFxThread(Navigator::showLogin).lookup(".card")).isNotNull();
    }

    @Test
    void loginViewLoadsWithNotice() throws Exception {
        assertThat(onJavaFxThread(navigator -> navigator.showLoginWithNotice("Aviso"))).isNotNull();
    }

    @Test
    void registerViewLoads() throws Exception {
        assertThat(onJavaFxThread(Navigator::showRegister).lookup(".card")).isNotNull();
    }

    @Test
    void mainViewLoadsWithTheEntriesScreenOpen() throws Exception {
        Parent main = onJavaFxThread(Navigator::showMain);

        assertThat(main.lookup(".top-bar")).isNotNull();
        assertThat(main.lookup("#table")).isNotNull();
    }
}
