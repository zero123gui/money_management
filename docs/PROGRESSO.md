# Progresso do Projeto

> Atualize este arquivo **ao fim de cada sessão**. Decisões e motivos ficam em `DECISOES.md`.

**Última atualização:** 2026-10-22

## Já feito

- [x] Primeiro commit do repositório.
- [x] `docs/DECISOES.md` criado com contexto, stack, modelo de acesso e modelo de dados.
- [x] Sessão de setup (2026-10-22):
  - [x] `.gitignore` criado (segredos, bancos locais, build, IDEs, SO).
  - [x] `AGENTS.md` criado (guia para agentes de IA).
  - [x] `docs/PROGRESSO.md` criado (este arquivo).
  - [x] Convenções de commits, branches, comentários e segredos registradas em `DECISOES.md`.
  - [x] Escapes quebrados do Markdown corrigidos em `DECISOES.md`.

## Próxima sessão

Ordem sugerida:

1. [ ] Revisar e aprovar as convenções propostas em `DECISOES.md` (commits, branches, idioma).
2. [ ] Decidir versões: Java (ex.: 21 LTS), Spring Boot, ferramenta de build (Maven ou Gradle).
3. [ ] Criar a estrutura do monorepo: `backend/` e `desktop/` (com `README.md` de cada um).
4. [ ] Substituir o `README.md` ("Hello") por descrição real do projeto e instruções de execução.
5. [ ] Criar `.env.example` / `application-example.properties` sem valores reais.
6. [ ] Backend: projeto Spring Boot, entidades `usuario` e `lancamento`, SQLite.
7. [ ] Backend: autenticação JWT e service centralizado de autorização.

## Backlog (depois)

- [ ] Backend: endpoints de resumo por categoria com regra de compartilhamento.
- [ ] Desktop: cliente JavaFX consumindo a API.
- [ ] Migração de SQLite para PostgreSQL.
- [ ] Grupo familiar: tabelas `grupo_familiar` e `grupo_membro`, chave `compartilha_resumo`.
- [ ] CI (build e testes) e proteção da branch `main`.
- [ ] Revisão de segurança antes de qualquer deploy.

## Pendências e dúvidas em aberto

- Banco: SQLite só para começar; definir quando migrar para PostgreSQL.
- Como a API será hospedada para uso em vários dispositivos (ainda não decidido).
