package br.com.financeiro.desktop.controller;

import br.com.financeiro.desktop.api.ApiClient;
import br.com.financeiro.desktop.api.ApiException;
import br.com.financeiro.desktop.api.dto.AlterarSenhaRequest;
import br.com.financeiro.desktop.api.dto.UsuarioResponse;
import br.com.financeiro.desktop.format.Formats;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;

/**
 * Dados da conta, troca de senha (RF-04) e exclusão da conta (RF-05).
 */
public class ContaController {

    private static final Map<String, String> LABELS = Map.of("senhaAtual", "Senha atual", "novaSenha", "Nova senha");
    private static final String NOT_LOADED = "—";

    private final ApiClient api;
    private final ApiTasks tasks;
    private final Navigator navigator;

    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label createdAtLabel;
    @FXML
    private Label accountErrorLabel;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmationField;
    @FXML
    private Label passwordNoticeLabel;
    @FXML
    private Label passwordErrorLabel;
    @FXML
    private Button changePasswordButton;
    @FXML
    private Button deleteAccountButton;
    @FXML
    private Label deleteErrorLabel;

    private UsuarioResponse user;

    /**
     * Cria o controller.
     *
     * @param api       cliente da API
     * @param tasks     executor das chamadas
     * @param navigator volta para o login depois de excluir a conta
     */
    public ContaController(ApiClient api, ApiTasks tasks, Navigator navigator) {
        this.api = api;
        this.tasks = tasks;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        accountErrorLabel.managedProperty().bind(accountErrorLabel.visibleProperty());
        passwordNoticeLabel.managedProperty().bind(passwordNoticeLabel.visibleProperty());
        passwordErrorLabel.managedProperty().bind(passwordErrorLabel.visibleProperty());
        deleteErrorLabel.managedProperty().bind(deleteErrorLabel.visibleProperty());
        // A confirmação da exclusão pede o e-mail, então só é liberada depois de carregá-lo.
        deleteAccountButton.setDisable(true);
        tasks.run(api::currentUser, this::showUser, failure -> {
            accountErrorLabel.setText(failure.describe(Map.of()));
            accountErrorLabel.setVisible(true);
        });
    }

    @FXML
    private void onChangePassword() {
        passwordNoticeLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);
        // Só esta regra fica no cliente: a API recebe a nova senha uma vez e não tem como compará-las.
        if (!newPasswordField.getText().equals(confirmationField.getText())) {
            showPasswordError("As novas senhas não são iguais.");
            return;
        }
        AlterarSenhaRequest request =
                new AlterarSenhaRequest(currentPasswordField.getText(), newPasswordField.getText());
        changePasswordButton.setDisable(true);
        tasks.run(() -> {
            api.changePassword(request);
            return true;
        }, changed -> {
            changePasswordButton.setDisable(false);
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmationField.clear();
            passwordNoticeLabel.setText("Senha alterada. Use a nova senha no próximo login.");
            passwordNoticeLabel.setVisible(true);
        }, this::onPasswordFailed);
    }

    @FXML
    private void onDeleteAccount() {
        deleteErrorLabel.setVisible(false);
        if (confirmDelete()) {
            deleteAccountButton.setDisable(true);
            tasks.run(() -> {
                api.deleteAccount();
                return true;
            }, deleted -> navigator.onAccountDeleted(), failure -> {
                deleteAccountButton.setDisable(false);
                deleteErrorLabel.setText(failure.describe(Map.of()));
                deleteErrorLabel.setVisible(true);
            });
        }
    }

    private void showUser(UsuarioResponse loaded) {
        user = loaded;
        nameLabel.setText(loaded.nome());
        emailLabel.setText(loaded.email());
        createdAtLabel.setText(loaded.criadoEm() == null ? NOT_LOADED : Formats.dateTime(loaded.criadoEm()));
        deleteAccountButton.setDisable(false);
    }

    private void onPasswordFailed(ApiException failure) {
        changePasswordButton.setDisable(false);
        showPasswordError(failure.describe(LABELS));
    }

    private void showPasswordError(String text) {
        passwordErrorLabel.setText(text);
        passwordErrorLabel.setVisible(true);
    }

    /**
     * A exclusão da conta é a única ação do aplicativo sem volta. Por isso, em vez
     * de um clique, exige digitar o e-mail: quem proíbe a exclusão de outra conta
     * continua sendo a API, e isto só evita um engano.
     */
    private boolean confirmDelete() {
        ButtonType delete = new ButtonType("Excluir minha conta", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(deleteAccountButton.getScene().getWindow());
        dialog.setTitle("Excluir conta");
        dialog.setHeaderText("Excluir sua conta e todos os seus dados?");
        dialog.setContentText("Lançamentos e categorias próprias serão apagados de vez.\nDigite "
                + user.email() + " para confirmar:");
        dialog.getDialogPane().getButtonTypes().setAll(delete, cancel);
        Node deleteNode = dialog.getDialogPane().lookupButton(delete);
        deleteNode.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !dialog.getEditor().getText().strip().equalsIgnoreCase(user.email()),
                dialog.getEditor().textProperty()));
        return dialog.showAndWait().isPresent();
    }
}
