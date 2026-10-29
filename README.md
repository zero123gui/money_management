# Controle Financeiro Familiar

Aplicação desktop para a família registrar e acompanhar receitas e despesas, usada em vários dispositivos. Cada pessoa tem login próprio, e seus lançamentos são privados. No futuro, membros de um grupo familiar poderão ver os resumos por categoria uns dos outros, se cada um permitir.

## Status

🟡 **Em desenvolvimento.** Backend com a estrutura inicial e o banco; ainda sem endpoints.

- [x] Decisões de arquitetura e convenções do projeto
- [x] Estrutura do monorepo (`backend/`, `desktop/`)
- [x] Requisitos e modelo de dados do MVP
- [x] Backend: projeto Spring Boot e migração inicial do banco
- [ ] Backend: API, autenticação e lançamentos
- [ ] Desktop: cliente JavaFX

Detalhes em [`docs/PROGRESSO.md`](docs/PROGRESSO.md).

## Stack

| Parte    | Tecnologia                                                        |
|----------|-------------------------------------------------------------------|
| Backend  | Java 21, Spring Boot 4.x, Spring Security (JWT), Maven            |
| Banco    | PostgreSQL (gerenciado na nuvem)                                  |
| Desktop  | Java 21, JavaFX, Maven (Windows; macOS no futuro)                 |

## Estrutura

```
financeiro/
├── backend/   API REST (Spring Boot)
├── desktop/   cliente JavaFX
├── docs/      decisões, requisitos e progresso
└── AGENTS.md  guia para agentes de IA
```

## Documentação

- [`docs/DECISOES.md`](docs/DECISOES.md): decisões de arquitetura e convenções.
- [`docs/REQUISITOS.md`](docs/REQUISITOS.md): requisitos funcionais e não funcionais.
- [`docs/API.md`](docs/API.md): contrato da API REST (convenções e endpoints).
- [`docs/PROGRESSO.md`](docs/PROGRESSO.md): o que foi feito e próximos passos.
- [`AGENTS.md`](AGENTS.md): guia para agentes de IA que trabalham no projeto.

## Como executar

Pré-requisitos: Java 21, Maven e PostgreSQL. Para compilar e testar tudo, na raiz: `mvn verify`.

Para rodar a API localmente, veja [`backend/README.md`](backend/README.md).

## Contribuindo

- Commits no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/), em português: `tipo(escopo): descrição`.
- Branches: `feature/…`, `fix/…`, `docs/…`, `refactor/…`, `chore/…`.
- Nada direto na `main`; mudanças entram por Pull Request.
