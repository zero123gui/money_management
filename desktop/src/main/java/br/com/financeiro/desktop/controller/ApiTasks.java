package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiException;
import br.com.financeiro.desktop.api.SessionExpiredException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

/**
 * Roda chamadas à API fora da thread do JavaFX, para a tela não travar, e
 * devolve o resultado nessa thread.
 */
public class ApiTasks {

    private static final Logger LOGGER = Logger.getLogger(ApiTasks.class.getName());
    private static final String UNEXPECTED_MESSAGE = "Ocorreu um erro inesperado. Tente novamente.";

    // Threads daemon não impedem o aplicativo de fechar durante uma chamada.
    private final Executor executor = Executors.newCachedThreadPool(work -> {
        Thread thread = new Thread(work, "api-call");
        thread.setDaemon(true);
        return thread;
    });
    private final Runnable onSessionExpired;

    /**
     * Cria o executor de chamadas.
     *
     * @param onSessionExpired ação comum a todas as telas quando o token deixa de valer
     */
    public ApiTasks(Runnable onSessionExpired) {
        this.onSessionExpired = onSessionExpired;
    }

    /**
     * Executa uma chamada em segundo plano. Os retornos rodam na thread do JavaFX,
     * e a sessão expirada não chega a {@code onFailure}.
     *
     * @param work      chamada à API
     * @param onSuccess recebe o resultado
     * @param onFailure recebe a falha
     * @param <T>       tipo do resultado
     */
    public <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<ApiException> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> handleFailure(task.getException(), onFailure));
        executor.execute(task);
    }

    private void handleFailure(Throwable failure, Consumer<ApiException> onFailure) {
        if (failure instanceof SessionExpiredException) {
            onSessionExpired.run();
        } else if (failure instanceof ApiException apiException) {
            onFailure.accept(apiException);
        } else {
            LOGGER.log(Level.WARNING, "Falha inesperada em uma chamada à API", failure);
            onFailure.accept(new ApiException(ApiException.NO_RESPONSE, UNEXPECTED_MESSAGE, List.of()));
        }
    }
}
