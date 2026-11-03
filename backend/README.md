# backend

API REST do Controle Financeiro Familiar.

- **Stack**: Java 21, Spring Boot 4.1.2, Spring Security com JWT, Maven, Flyway.
- **Banco**: PostgreSQL (gerenciado na nuvem em produção).
- **Responsabilidade**: autenticação, regras de negócio e isolamento dos dados por usuário. Toda consulta filtra pelo usuário autenticado.

**Status:** projeto Spring Boot, migração `V1`, entidades JPA, repositories e autenticação JWT criados. Endpoints prontos: `POST /auth/cadastro`, `POST /auth/login`, `GET` e `DELETE /usuarios/me` e `PUT /usuarios/me/senha`.

## Como executar localmente

1. Crie o banco `financeiro` e o usuário `financeiro_app` no PostgreSQL local.
2. Copie `.env.example` para `.env` (nesta pasta) e preencha `DB_PASSWORD` e `JWT_SECRET`. O `.env` não vai para o Git.
3. Compile e rode:

   ```bash
   mvn spring-boot:run
   ```

   Na primeira execução, o Flyway cria as tabelas e as categorias padrão.

Para compilar e testar tudo a partir da raiz do repositório: `mvn verify`. Os testes de integração usam Testcontainers, então o Docker Desktop precisa estar aberto; o banco local não é usado.

## Estrutura

```
src/main/java/br/com/financeiro/backend/   código da API
src/main/resources/application.properties  configuração (sem segredos)
src/main/resources/db/migration/           migrações do Flyway (V1, V2…)
```

Decisões em [`../docs/DECISOES.md`](../docs/DECISOES.md) e requisitos em [`../docs/REQUISITOS.md`](../docs/REQUISITOS.md).
