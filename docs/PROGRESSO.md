# Progresso do Projeto

> Atualize este arquivo **ao fim de cada sessão**. Decisões e motivos ficam em `DECISOES.md`.

**Última atualização:** 2026-10-28

## Já feito

- [x] Primeiro commit do repositório.
- [x] `docs/DECISOES.md` criado com contexto, stack, modelo de acesso e modelo de dados.
- [x] Sessão de setup (2026-10-22):
  - [x] `.gitignore` criado (segredos, bancos locais, build, IDEs, SO).
  - [x] `AGENTS.md` criado (guia para agentes de IA).
  - [x] `docs/PROGRESSO.md` criado (este arquivo).
  - [x] Convenções de commits, branches, comentários e segredos registradas em `DECISOES.md`.
  - [x] Escapes quebrados do Markdown corrigidos em `DECISOES.md`.
- [x] Sessão de planejamento (2026-10-28):
  - [x] Convenções aprovadas pelo usuário.
  - [x] Regra "agentes não fazem commit, só staging" registrada em `AGENTS.md` e `DECISOES.md`.
  - [x] Decididos Java 21, Spring Boot 4.x e Maven (motivos em `DECISOES.md`).
  - [x] Estrutura do monorepo criada: `backend/` e `desktop/`, cada uma com `README.md`.
  - [x] `README.md` da raiz reescrito com a descrição e o status do projeto.
  - [x] `docs/REQUISITOS.md` criado com requisitos funcionais e não funcionais.
  - [x] Todas as 11 perguntas em aberto respondidas.
  - [x] Decidido PostgreSQL desde o início (substitui "SQLite para começar") e API e banco na nuvem.
  - [x] Modelo de dados do MVP fechado em `DECISOES.md`: `usuario`, `categoria` (nova tabela), `lancamento` com `tipo` e exclusão lógica.
  - [x] Branch `docs/setup-e-requisitos` publicada no GitHub e PR aberto pelo usuário.

## Próxima sessão

Ordem sugerida:

1. [ ] Validar os requisitos que ainda estão como "Proposto" em `docs/REQUISITOS.md`.
2. [ ] Decidir como rodar o PostgreSQL no computador de desenvolvimento (instalação direta no Windows ou Docker).
3. [ ] Decidir a ferramenta de migração do banco (sugestão: Flyway, que versiona a criação das tabelas em arquivos SQL).
4. [ ] Desenhar os endpoints da API do MVP (rotas, entradas e saídas) antes de escrever código.
5. [ ] Definir o tempo de expiração do token JWT.

## Backlog (depois)

- [ ] Criar o `pom.xml` agregador na raiz (Maven multimódulo).
- [ ] Backend: projeto Spring Boot, entidades e migrações.
- [ ] Backend: autenticação JWT e service centralizado de autorização.
- [ ] Backend: endpoints de lançamentos, categorias, saldo e resumo por categoria.
- [ ] Criar `.env.example` / `application-example.properties` quando surgir a primeira configuração.
- [ ] Desktop: definir a versão do JavaFX e criar o cliente.
- [ ] Escolher o provedor de nuvem e publicar a API e o banco (com HTTPS).
- [ ] CI (build e testes) e proteção da branch `main`.
- [ ] Revisão de segurança antes de qualquer deploy.
- [ ] Pós-MVP: forma de pagamento, recorrência, parcelas, exportação CSV, limite por categoria.
- [ ] Pós-MVP: versão para macOS.
- [ ] Futuro: grupo familiar (`grupo_familiar`, `grupo_membro`, `compartilha_resumo`).

## Pendências e dúvidas em aberto

- Provedor de nuvem para a API e o banco.
- Versão do JavaFX.
