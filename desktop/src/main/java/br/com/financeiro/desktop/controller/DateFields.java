package br.com.financeiro.desktop.controller;

import java.time.LocalDate;
import javafx.scene.control.DatePicker;

/**
 * Apoio aos campos de data das telas.
 */
final class DateFields {

    private DateFields() {
    }

    /**
     * Confirma o texto digitado no campo. O campo só faz isso ao perder o foco, e
     * o Enter aciona o botão padrão sem tirar o foco dele; sem esta chamada, a
     * tela usaria a data antiga.
     *
     * @param picker campo de data
     * @return {@code false} se o texto não for uma data válida
     */
    static boolean commitTyped(DatePicker picker) {
        String text = picker.getEditor().getText();
        LocalDate typed = picker.getConverter().fromString(text);
        if (typed == null && text != null && !text.isBlank()) {
            return false;
        }
        picker.setValue(typed);
        return true;
    }
}
