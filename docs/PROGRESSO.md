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
  - [x] `docs/REQUISITOS.md` criado: requisitos funcionais e não funcionais, dados derivados e perguntas em aberto.
  - [x] Decidido não criar `.env.example` ou `.properties` por enquanto, porque ainda não há variáveis de ambiente.
  - [x] Respondidas as perguntas 1, 9 e 10 de `REQUISITOS.md`: o app registra receitas e despesas, o desktop exige internet, e API e banco ficam na nuvem.
  - [x] Seção "Hospedagem" criada em `DECISOES.md`.

## Próxima sessão

Ordem sugerida:

1. [ ] Responder as perguntas que faltam em `docs/REQUISITOS.md`. A principal é a 2 (categoria como tabela); a 11 (PostgreSQL desde o início) também é nova.
2. [ ] Revisar os requisitos e marcar cada um como Aprovado, Alterado ou Descartado.
3. [ ] Fechar o modelo de dados (`usuario`, `lancamento`, `categoria`?) e registrá-lo em `DECISOES.md`.
4. [ ] Definir o escopo da v1 a partir dos requisitos "Essencial".

## Backlog (depois)

- [ ] Criar o `pom.xml` agregador na raiz (Maven multimódulo).
- [ ] Backend: projeto Spring Boot, entidades, SQLite, migrações.
- [ ] Backend: autenticação JWT e service centralizado de autorização.
- [ ] Backend: endpoints de lançamentos, saldo e resumo por categoria.
- [ ] Criar `.env.example` / `application-example.properties` quando surgir a primeira configuração.
- [ ] Desktop: definir a versão do JavaFX e criar o cliente.
- [ ] Migração de SQLite para PostgreSQL.
- [ ] Grupo familiar: tabelas `grupo_familiar` e `grupo_membro`, chave `compartilha_resumo`.
- [ ] CI (build e testes) e proteção da branch `main`.
- [ ] Revisão de segurança antes de qualquer deploy.
- [ ] Escolher o provedor de nuvem e publicar a API e o banco (com HTTPS).

## Pendências e dúvidas em aberto

- Ver a seção "Perguntas em aberto" de `docs/REQUISITOS.md`.
- Provedor de nuvem para a API e o banco.
- Branch de trabalho ainda não publicada no GitHub (diagnóstico de 2026-10-28: o remoto só tem `main`).
