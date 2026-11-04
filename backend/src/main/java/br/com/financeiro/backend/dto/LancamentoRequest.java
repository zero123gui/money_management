package br.com.financeiro.backend.dto;

import br.com.financeiro.backend.entity.Tipo;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Corpo de {@code POST /lancamentos} e {@code PUT /lancamentos/{id}}.
 *
 * @param tipo        tipo do lançamento, igual ao da categoria
 * @param valor       valor positivo, com até 2 casas decimais
 * @param data        data do lançamento
 * @param descricao   descrição opcional
 * @param categoriaId identificador da categoria
 */
public record LancamentoRequest(
        @NotNull(message = "é obrigatório")
        Tipo tipo,

        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser maior que zero")
        @Digits(integer = 13, fraction = 2, message = "deve ter até 13 dígitos e 2 casas decimais")
        BigDecimal valor,

        @NotNull(message = "é obrigatória")
        LocalDate data,

        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String descricao,

        @NotNull(message = "é obrigatória")
        Long categoriaId) {
}
