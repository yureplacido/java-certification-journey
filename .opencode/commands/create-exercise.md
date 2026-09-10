---
description: Cria um exercício no estilo 1Z0-830 sobre um tópico, partindo do conteúdo já existente no repositório. Use para praticar um tópico com exercícios direcionados.
agent: java-certification
---

Crie um exercício de estudo no estilo da certificação 1Z0-830.

Tópico: $ARGUMENTS (ex.: generics, streams, sealed types; se vazio, proponha o próximo tópico
previsto em `docs/progress.yaml` e no roadmap).

Bases obrigatórias (consulte antes de criar):
- Notas e exemplos existentes no diretório do tópico (ex.: `generics/`).
- `docs/roadmap/java-21.md` — objetivo oficial relacionado.
- `docs/traps/` — pegadinhas de exame do tópico.

Entregue:
1. Enunciado do exercício e objetivo oficial que ele cobre.
2. Resolução comentada, diferenciando compile-time, runtime, JLS, Java API, JVM e pegadinha de
   certificação quando aplicável.
3. Proposta de onde salvar (diretório do tópico, `src/main/java/com/placido/certification/<tópico>/`
   para código e `src/test/java/...` para a validação JUnit).

Regras do `AGENTS.md`: não invente regras da linguagem; em comportamento dúbio, proponha um
experimento mínimo antes de afirmar. Baseline Java 21.