package br.com.financeiro.backend.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * Senha aceita pela API: de 8 caracteres a 72 bytes. O limite é em bytes porque
 * o BCrypt só aceita até 72, e letras acentuadas ocupam mais de um byte.
 * Valor nulo ou em branco é recusado por {@code @NotBlank}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPassword.Validator.class)
public @interface ValidPassword {

    /** @return mensagem exibida ao usuário */
    String message() default "deve ter no mínimo 8 caracteres e no máximo 72 bytes";

    /** @return grupos de validação */
    Class<?>[] groups() default {};

    /** @return dados adicionais da restrição */
    Class<? extends Payload>[] payload() default {};

    /** Aplica a regra de {@link ValidPassword}. */
    class Validator implements ConstraintValidator<ValidPassword, String> {

        private static final int MIN_CHARS = 8;
        private static final int MAX_BYTES = 72;

        /**
         * Confere o tamanho da senha.
         *
         * @param value   senha recebida
         * @param context contexto da validação
         * @return {@code true} se a senha estiver no tamanho aceito ou em branco
         */
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true;
            }
            return value.length() >= MIN_CHARS && value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
        }
    }
}
