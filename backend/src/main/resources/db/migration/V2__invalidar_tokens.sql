-- Invalidação de tokens ao trocar a senha e ao sair (docs/DECISOES.md, "Modelo de acesso").

-- Muda a cada troca de senha. O token guarda a versão de quando foi emitido,
-- então os tokens antigos deixam de valer todos de uma vez.
ALTER TABLE usuario ADD COLUMN versao_token INTEGER NOT NULL DEFAULT 0;

-- Tokens encerrados pelo logout antes de expirar. Basta guardá-los até a
-- expiração: depois disso a assinatura já os recusa.
CREATE TABLE token_revogado (
    id         UUID        PRIMARY KEY,
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    expira_em  TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_token_revogado_expira_em ON token_revogado (expira_em);
