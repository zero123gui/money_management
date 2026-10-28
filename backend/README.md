# backend

API REST do Controle Financeiro Familiar.

- **Stack**: Java 21, Spring Boot 4.x, Spring Security com JWT, Maven.
- **Banco**: PostgreSQL (gerenciado na nuvem em produção).
- **Responsabilidade**: autenticação, regras de negócio e isolamento dos dados por usuário. Toda consulta filtra pelo usuário autenticado.

**Status:** ainda não criado. Esta pasta reserva o lugar do módulo no monorepo.

Decisões em [`../docs/DECISOES.md`](../docs/DECISOES.md) e requisitos em [`../docs/REQUISITOS.md`](../docs/REQUISITOS.md).
