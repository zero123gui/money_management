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
  - [x] Decidido: desenvolver e testar localmente antes da nuvem, PostgreSQL instalado no Windows, JWT de 1 hora e JavaFX 21 (LTS).
  - [x] `docs/API.md` criado com as convenções REST e os 17 endpoints do MVP.
  - [x] Aprovado: URLs e campos JSON da API em português.
  - [x] Todos os requisitos de `REQUISITOS.md` aprovados pelo usuário.
  - [x] `docs/API.md` aprovado (paginação delegada ao agente).
  - [x] Flyway escolhido para as migrações do banco.
  - [x] Categorias padrão definidas (falta o tipo de "Investimentos").
  - **Fase de planejamento concluída.** Próxima fase: código do backend.

## Próxima sessão

Ordem sugerida:

1. [ ] Definir o tipo da categoria "Investimentos" (ver `DECISOES.md`, "Categorias padrão").
2. [ ] Fazer o merge do PR de documentação na `main` e criar a branch `feature/backend-estrutura-inicial` a partir dela.
3. [ ] Criar no PostgreSQL local um banco e um usuário para o projeto. A senha fica fora do Git (variável de ambiente).
4. [ ] Criar o `pom.xml` agregador na raiz e o projeto Spring Boot em `backend/`.
5. [ ] Criar a migração `V1` do Flyway: tabelas `usuario`, `categoria`, `lancamento` e categorias padrão.
6. [ ] Criar o `.env.example` com as variáveis necessárias (banco e chave JWT), sem valores reais.

## Backlog (depois)

- [ ] Backend: entidades JPA e repositories.
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
