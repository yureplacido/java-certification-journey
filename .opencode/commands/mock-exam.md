---
description: Monta (ou revisa) um simulado no estilo 1Z0-830 em exams/, priorizando as pegadinhas de docs/traps/. Use para praticar com prova completa e cronometrada.
agent: exam-designer
---

Monte um simulado no estilo real do exame 1Z0-830.

Prova/escopo: $ARGUMENTS (ex.: `mock-01` com foco em streams; se vazio, escolha o próximo simulado
pendente em `docs/progress.yaml` e a distribuição pelos tópicos mais fracos).

Bases obrigatórias (consulte antes):
- `docs/roadmap/java-21.md` — distribuição por objetivos oficiais.
- `docs/traps/` — pegadinhas a explorar nas questões.
- Questões já existentes em `exams/` — reutilize e evite duplicação.
- `docs/error-log.md` e `docs/progress.yaml` — lacunas para cobrir.

Entregue:
1. Simulado com N questões de múltipla escolha (mínimo 20) cobrindo ≥ 5 seções do exame, com
   gabarito e explicações comentadas (diferenciando compile-time, runtime, JLS, Java API, JVM e
   a pegadinha explorada).
2. Validação do código de cada questão: compile/execute conforme a resposta afirmada; ajuste a
   questão se o comportamento real divergir.
3. Menção explícita das traps exploradas em `docs/traps/`.

Regras do `AGENTS.md`: baseline Java 21; não invente scores — registre apenas resposta/gabarito;
questões novas que revelarem pegadinhas não documentadas vão para `docs/traps/`.