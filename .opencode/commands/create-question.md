---
description: Cria uma questão de múltipla escolha no estilo 1Z0-830 usando as pegadinhas de docs/traps/. Use para alimentar os simulados com questões direcionadas.
agent: exam-designer
---

Crie uma questão de múltipla escolha no estilo real do exame 1Z0-830.

Alvo e escopo: $ARGUMENTS (ex.: tópico, trap específica em `docs/traps/`, ou destino em `exams/`;
se vazio, escolha uma pegadinha ainda não explorada).

Bases obrigatórias (consulte antes):
- `docs/traps/` — matéria-prima de pegadinhas (não invente comportamento).
- `docs/roadmap/java-21.md` — objetivo oficial coberto pela questão.
- Questões já existentes em `exams/` — evite duplicação.

Entregue:
1. Questão com enunciado, 4-5 opções (só uma correta), resposta correta e explicação comentada.
2. Explicação diferenciando compile-time, runtime, JLS, Java API, JVM e a pegadinha explorada.
3. Validação de qualquer código da questão: compile/execute conforme o comportamento afirmado.
4. Proposta de local de gravação (diretório de `exams/`; traps novas vão para `docs/traps/`).

Regras do `AGENTS.md`: baseline Java 21; nunca invente scores (gabarito sim, desempenho não);
não apague questões existentes.