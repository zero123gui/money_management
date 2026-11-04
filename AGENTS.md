# AGENTS.md: guia para agentes de IA

Aplicação de **controle financeiro familiar**: backend Spring Boot (API REST + JWT) e cliente desktop JavaFX, em monorepo (`backend/`, `desktop/`). Os dados financeiros são privados por usuário.

## Leia antes de qualquer tarefa

1. `docs/DECISOES.md`: decisões de arquitetura e convenções. É a fonte da verdade.
2. `docs/PROGRESSO.md`: o que já foi feito e o que fazer na próxima sessão.
3. `docs/REQUISITOS.md`: requisitos funcionais e não funcionais, e os dados que derivam deles.
4. `docs/API.md`: contrato da API REST. Todo endpoint novo ou alterado deve ser refletido nele no mesmo commit.

Se a tarefa conflitar com uma decisão registrada, **pare e pergunte** ao usuário. Não mude a decisão por conta própria.

## Regras inegociáveis

- **Segredos**: nunca commitar nem escrever em arquivo versionado chaves de API, tokens, senhas, chave JWT, bancos locais (`*.db`) ou dados financeiros reais. Use variáveis de ambiente e arquivos `*.example`. Se encontrar um segredo exposto, avise o usuário.
- **Privacidade**: todo acesso a `lancamento` deve filtrar pelo usuário autenticado, no backend. O cliente nunca decide o que exibir. A autorização fica no service centralizado, sem regras duplicadas em controllers.
- **Grupo familiar**: só resumos por categoria, nunca lançamentos individuais.
- **Nada de commits direto na `main`.** Trabalhe em branch e abra Pull Request.
- **Agentes não fazem commit.** Coloque os arquivos em staging (`git add`) e entregue ao usuário a mensagem de commit sugerida, no padrão do projeto, para ele revisar e commitar. Só faça o commit se o usuário pedir explicitamente naquela mensagem.
- Não faça `push`, `force push`, nem apague branches sem pedido explícito.

## Convenções

Detalhes e motivos em `docs/DECISOES.md`, seção "Convenções do projeto".

- **Idioma**: documentação, commits e comentários em português; identificadores de código em inglês. Exceções: banco de dados, URLs e campos JSON da API ficam em português sem acento (ex.: `/lancamentos`, `categoriaId`), e os DTOs seguem os nomes do JSON.
- **Commits** (Conventional Commits): `tipo(escopo): descrição no imperativo`. Tipos: `feat fix docs style refactor test chore build ci perf`. Escopos: `backend desktop docs repo`.
- **Branches**: `feature/…`, `fix/…`, `docs/…`, `refactor/…`, `chore/…`, em kebab-case.
- **Comentários**: Javadoc em classes e métodos públicos; comentário inline explica o *porquê*; sem código comentado.
- Siga o estilo do código ao redor. Mudanças pequenas e focadas, um assunto por commit.

## Fluxo de trabalho de uma sessão

1. Ler `DECISOES.md` e `PROGRESSO.md`.
2. Criar a branch com o nome padronizado.
3. Implementar com testes quando houver lógica.
4. Registrar novas decisões em `docs/DECISOES.md` (data + motivo).
5. Atualizar `docs/PROGRESSO.md`: marcar o que foi feito e listar a próxima sessão.
6. Colocar os arquivos em staging, entregar a mensagem de commit sugerida e resumir o resultado ao usuário, dizendo o que não foi verificado.

## Perfil do usuário

Experiente em Java, pouco familiar com Python, Electron e Tauri. Prefira soluções no ecossistema Java e explique brevemente escolhas fora dele.

## Estado atual

Planejamento concluído: requisitos, modelo de dados e contrato da API aprovados. O backend tem o projeto Spring Boot (Maven multimódulo a partir da raiz), a migração `V1` do Flyway, as entidades JPA, os repositories e a autenticação JWT, com os 17 endpoints do MVP (`/auth/*`, `/usuarios/me`, `/categorias`, `/lancamentos` e `/relatorios/*`). Os testes de integração usam Testcontainers: o Docker Desktop precisa estar aberto para rodar `mvn verify`. O `desktop/` tem o projeto JavaFX com todas as telas do MVP, e os testes dele não precisam de Docker. Ainda não há publicação na nuvem nem instalador. Veja `docs/PROGRESSO.md`.
