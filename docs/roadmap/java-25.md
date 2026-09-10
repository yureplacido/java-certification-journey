# Roadmap Java 25 — estudar como delta (fase 2)

Java 25 (LTS, lançado em set/2025) é estudado **depois** da certificação 1Z0-830, como uma
camada sobre Java 21. O foco não é reaprender a linguagem, e sim **o que mudou entre 21 e 25**.

## Estratégia

1. Completar a fase Java 21 (1Z0-830) antes.
2. Para cada feature, comparar com Java 21 e anotar o delta em `java25/delta/`.
3. Praticar migração de código 21 → 25 em `java25/migration/` (projeções de código real).
4. Consolidar em cheat-sheets próprias.

## Destaques 22 → 25 (para validar e explorar)

**Finalizados no JDK 25**
- JEP 506 — Scoped Values
- JEP 510 — Key Derivation Function API
- JEP 511 — Module Import Declarations
- JEP 512 — Compact Source Files e Instance Main Methods
- JEP 513 — Flexible Constructor Bodies
- JEP 519 — Compact Object Headers
- JEP 521 — Generational Shenandoah

**Previews/incubators no JDK 25 (estudar por compreensão, sem usar em produção)**
- JEP 505 — Structured Concurrency (5º preview)
- JEP 502 — Stable Values (preview)
- JEP 507 — Primitive Types in Patterns, `instanceof` e `switch` (3º preview)
- JEP 508 — Vector API (10º incubator)

**Também relevante (releases 22–24, já estáveis no 25)**
- JEP 447 — Statements before `super(...)` (construtor flexível, evoluiu para o 513)
- Stream Gatherers (finalizado no JDK 24)
- `implicitly declared classes` / método `main` simplificado (evoluiu para o 512)

> Lista completa por release em https://openjdk.org/projects/jdk/25/ — conferir status atual antes de estudar cada JEP.

## Mapeamento para o laboratório

| Tema | Dir. principal |
| --- | --- |
| Concorrência (structured concurrency, scoped values, virtual threads) | `java25/delta/`, `concurrency/` |
| Linguagem (flexible bodies, compact sources, module imports) | `java25/language/` |
| APIs (KDF, Vector, Stream Gatherers) | `java25/apis/` |
| Migração 21 → 25 (remocões/deprecações, mudanças de comportamento) | `java25/migration/` |

## Status

Em espera — entra após a fase 1Z0-830.