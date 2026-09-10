# Roadmap de 16 semanas — Java SE 21 Developer Professional (1Z0-830)

Plano de alto nível para um arquiteto experiente: rápido em fundamentos, profundo em
pegadinhas/novidades. Este é um esqueleto — a agenda detalhada de cada semana será refinada
conforme o conteúdo de cada tópico for criado.

- Baseline: Java 21. Fase 2 (Java 25 delta) acontece depois, fora destas 16 semanas.
- Prova diagnóstica antes de começar para calibrar o ponto de partida.
- Os simulados marcam as revisões dirigidas.

## Semanas

| Semana | Foco | Entregável |
| --- | --- | --- |
| 1 | Setup + diagnóstico + navegação pelos objetivos 1Z0-830 | `exams/diagnostic/` concluído; pontos fracos mapeados |
| 2 | Seção 1 — tipos, `Math`, casting/precedência, `String`/`StringBuilder`/text blocks, Date-Time | notas + testes em `language/`, `datetime/` |
| 3 | Seção 2 — flow control (`switch` statement/expression, loops, labels) | notas + testes em `language/` |
| 4 | Seção 3 — classes, records, nested classes, inicializadores, `var`, encapsulação | notas + testes em `oop/` |
| 5 | Seção 3 — herança, polimorfismo, sealed types, pattern matching (`instanceof`/`switch`), interfaces, enums | notas + testes em `oop/` |
| 6 | Seção 4 — exceções, try-with-resources, multi-catch | notas + testes em `exceptions/` |
| 7 | Seção 5 — arrays, coleções, ordenação | notas + testes em `collections/` |
| 8 | Seção 6 — lambdas, functional interfaces, method references | notas + testes em `functional/` |
| 9 | Seção 6 — streams: pipeline, collectors, paralelismo | notas + testes em `streams/` |
| 10 | Seção 7 — módulos JPMS, packaging, migration | notas + testes em `modules/` |
| 11 | Seção 8 — concorrência: threads (platform/virtual), executors, locking, parallel streams | notas + testes em `concurrency/` |
| 12 | Seções 9 e 10 — I/O, NIO.2, serialização, Localization | notas + testes em `io/`, `datetime/` |
| 13 | Revisão dirigida: `docs/traps/`, `docs/cheat-sheets/`, `docs/error-log.md` consolidados | material de revisão em dia |
| 14 | `exams/mock-01/` + `exams/mock-02/` | relatório de notas e lacunas |
| 15 | `exams/mock-03/` + revisão das lacunas apontadas | plano de correção executado |
| 16 | `exams/final/` + preparação logística do exame | decisão de agendar o exame real |

## Regras

- Semana avança por **evidência** (testes/notas), não por calendário: ao terminar um tópico,
  atualizar `docs/progress.yaml`.
- Qualquer semana estourando o prazo recalibra o cronograma (reduzir revisão, não a profundidade
  dos tópicos centrais).

## Status

Fundação criada. Semana 1 em aberto — aguarda prova diagnóstica.