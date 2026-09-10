# Roadmap Java 21 — baseline (1Z0-830)

Certificação: **Oracle Certified Professional: Java SE 21 Developer** — exame **1Z0-830**
(50 questões, 120 min, múltipla escolha; nota de corte ~68% — confirmar na página oficial).

Os objetivos oficiais do exame estão organizados em 10 seções. Este arquivo é o mapa de estudo:
para cada seção, os diretórios do laboratório onde o conteúdo é praticado. Conteúdo detalhado de
cada tópico será criado ao longo do estudo e refletido em `docs/progress.yaml`.

## Seções oficiais → diretórios

| # | Seção oficial (resumo) | Dir. de estudo |
| --- | --- | --- |
| 1 | Handl. Date, Time, Text, Numeric and Boolean Values — primitivas, wrappers, `Math`, precedência/casting, `String`/`StringBuilder`/text blocks, Date-Time API (DTS) | `language/`, `datetime/` |
| 2 | Controlling Program Flow — `if/else`, `switch` (statement/expression), loops, `break`/`continue` | `language/` |
| 3 | Using Object-Oriented Concepts — classes, records, nested classes, inicializadores, varargs/overload, encapsulação/imutabilidade/`var`, herança, sealed types, `instanceof` + pattern matching, interfaces, enums | `oop/` |
| 4 | Handling Exceptions — `try/catch/finally`, try-with-resources, multi-catch, exceções customizadas | `exceptions/` |
| 5 | Working with Arrays and Collections — arrays, `List`, `Set`, `Map`, `Deque`; ordenação | `collections/` |
| 6 | Working with Streams and Lambda Expressions — lambdas/functional interfaces, streams object e primitivas, redução/agrupamento/partição, paralelismo | `functional/`, `streams/` |
| 7 | Packaging and Deploying Java Code — módulos JPMS (`requires`, `exports`, serviços), jars modulares/não modulares, images `jlink`, migração/automatic modules | `modules/` |
| 8 | Managing Concurrent Code Execution — platform e **virtual threads**, `Runnable`/`Callable`, executors, locking/concurrent API, coleções concorrentes, parallel streams | `concurrency/` |
| 9 | Using Java I/O API — I/O streams (console/arquivo), serialização, `Path`/NIO.2 | `io/` |
| 10 | Implementing Localization — `Locale`, resource bundles, formatação de mensagens/datas/números/moeda | `datetime/` *(sem dir. próprio)* |

## Também é esperado do candidato (não é seção própria)

- Básico do Java Logging API (ex.: `java.util.logging`).
- Anotações: `@Override`, `@FunctionalInterface`, `@Deprecated`, `@SuppressWarnings`, `@SafeVarargs`.
- Generics, incluindo wildcards.

## Fora dos objetivos 1Z0-830 (mantidos por relevância profissional)

- **JDBC** (`jdbc/`) — removido dos objetivos do exame 21; mantido pelo uso profissional.
- **Reflection** (`reflection/`) — não cobrado; mantido por relevância prática (módulos: *deep
  reflection* aparece indiretamente em `exports`/`opens`).

## Tópicos da Java 21 que exigem profundidade (forte presença no exame)

- Records (compact constructors, validação, serialização)
- Sealed types (`sealed`, `permits`, exhaustive switch)
- Pattern matching (`instanceof` e `switch`; guards, dominance, `null`)
- Virtual threads (modelo de plataforma vs. virtual, pinning, estrutura)
- Sequenced collections (`SequencedCollection`, `SequencedMap`, `reversed()`, etc.)
- Text blocks (espaçamento incidental, escapes)
- Try-with-resources (ordem de fechamento, suppressed exceptions)
- Generics e wildcards (invariância, PECS, erasure, bridges)
- Módulos JPMS (incl. migration/automatic/unnamed modules)
- Streams sequenciais e paralelos (lazy, `Spliterator`, redução, collectors agrupando/particionando)

## Status

Fundação criada — nenhum tópico iniciado. Ver `docs/progress.yaml`.