package br.com.financeiro.backend.dto;

import br.com.financeiro.backend.entity.Tipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /categorias}.
 *
 * @param nome nome da categoria
 * @param tipo tipo da categoria, que não muda depois de criado
 */
public record CriarCategoriaRequest(
        @NotBlank(message = "é obrigatório")
        @Size(max = 50, message = "deve ter no máximo 50 caracteres")
        String nome,

        @NotNull(message = "é obrigatório")
        Tipo tipo) {
}
