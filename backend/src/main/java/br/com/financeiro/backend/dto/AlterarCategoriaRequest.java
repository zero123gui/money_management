package br.com.financeiro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code PUT /categorias/{id}}.
 *
 * @param nome  novo nome da categoria
 * @param ativa se a categoria aparece nas opções de novos lançamentos
 */
public record AlterarCategoriaRequest(
        @NotBlank(message = "é obrigatório")
        @Size(max = 50, message = "deve ter no máximo 50 caracteres")
        String nome,

        @NotNull(message = "é obrigatório")
        Boolean ativa) {
}
