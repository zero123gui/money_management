# Progresso do Projeto

> Atualize este arquivo **ao fim de cada sessão**. Decisões e motivos ficam em `DECISOES.md`.

**Última atualização:** 2026-10-29

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
  - [x] Decidido: desenvolver e testar localmente antes da nuvem, PostgreSQL instalado no Windows, JWT de 1 hora e JavaFX 21 (LTS).
  - [x] `docs/API.md` criado com as convenções REST e os 17 endpoints do MVP.
  - [x] Aprovado: URLs e campos JSON da API em português.
  - [x] Todos os requisitos de `REQUISITOS.md` aprovados pelo usuário.
  - [x] `docs/API.md` aprovado (paginação delegada ao agente).
  - [x] Flyway escolhido para as migrações do banco.
  - [x] Categorias padrão definidas; "Investimentos" é despesa e os rendimentos entram em "Receitas".
  - **Fase de planejamento concluída.** Próxima fase: código do backend.
- [x] Sessão de estrutura do backend (2026-10-29):
  - [x] PR de documentação (#1) mergeado na `main`.
  - [x] Branch `feature/backend-estrutura-inicial` criada a partir da `main`.
  - [x] `pom.xml` agregador na raiz e projeto Spring Boot 4.1.2 em `backend/` (pacote `br.com.financeiro.backend`).
  - [x] Migração `V1__criar_tabelas_iniciais.sql`: tabelas `usuario`, `categoria`, `lancamento` e as 8 categorias padrão.
  - [x] `backend/.env.example` versionado e `backend/.env` local (fora do Git) lido pelo Spring.
  - [x] `mvn verify` passa. A API sobe e tenta conectar no PostgreSQL local.
  - [x] Banco `financeiro` e usuário `financeiro_app` criados pelo usuário; `DB_PASSWORD` preenchido no `.env`.
  - [x] API executada: o Flyway aplicou a `V1` e as tabelas foram conferidas pelo usuário.
  - [x] RNF-10 alterado: datas de criação e alteração só em `lancamento` e `usuario`; `categoria` não precisa.

## Próxima sessão

Ordem sugerida:

1. [ ] Abrir o PR de `feature/backend-estrutura-inicial`.
2. [ ] Backend: entidades JPA e repositories (`Usuario`, `Categoria`, `Lancamento`), validadas pelo `ddl-auto=validate`.
3. [ ] Definir como os testes de integração acessam o banco (banco de teste no PostgreSQL local ou Testcontainers, que exige Docker).

## Backlog (depois)

- [ ] Backend: autenticação JWT e service centralizado de autorização.
- [ ] Backend: endpoints de lançamentos, categorias, saldo e resumo por categoria, conforme `docs/API.md`.
- [ ] Desktop: criar o cliente JavaFX 21.
- [ ] Depois do MVP rodando localmente: escolher o provedor de nuvem e publicar a API e o banco (com HTTPS).
- [ ] CI (build e testes) e proteção da branch `main`.
- [ ] Revisão de segurança antes de qualquer deploy.
- [ ] Pós-MVP: forma de pagamento, recorrência, parcelas, exportação CSV, limite por categoria.
- [ ] Pós-MVP: versão para macOS.
- [ ] Futuro: grupo familiar (`grupo_familiar`, `grupo_membro`, `compartilha_resumo`).

## Pendências e dúvidas em aberto

- Provedor de nuvem para a API e o banco (decidir depois do MVP local).
