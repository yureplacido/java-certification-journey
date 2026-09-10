# AGENTS.md

## Contexto

- Laboratório pessoal de preparação para certificação Java — exame **1Z0-830** (Oracle Certified Professional: Java SE 21 Developer).
- **Java 21 é a baseline.** Java 25 será estudado depois, como delta (`java25/`). Não usar código Java 22+ durante a fase 1Z0-830.
- Estudante: **Software Architect** sênior (~15 anos de Java). Conteúdo profundo e focado em exame; nada introdutório.
- **Foco em Java SE**, não Spring. Sem Spring, sem frameworks.

## Rigor da linguagem

- **Nunca inventar regras da linguagem.** Toda afirmação tem base na JLS/API ou é verificada.
- Na dúvida sobre comportamento, **criar experimento mínimo** (rodável com JUnit) antes de afirmar ou registrar.
- Diferenciar sempre, em notas e explicações:
  - **Compile-time:** erros de compilação e tipagem.
  - **Runtime:** exceções e comportamento em execução.
  - **JLS:** a especificação da linguagem.
  - **Java API:** comportamento das bibliotecas do JDK.
  - **JVM:** comportamento da máquina virtual (memória, threads, execução).
  - **Certificação:** conhecimento específico do exame (pegadinhas, formato).

## Metodologia (por tópico)

**Concept → Rule → Example → Experiment → Test → Trap → Question → Review → Error Log**

1. **Concept** — definir o conceito, direto ao ponto.
2. **Rule** — a regra que governa (JLS/API) e suas exceções.
3. **Example** — exemplo pequeno e executável.
4. **Experiment** — teste mínimo para validar comportamento dúbio; não confiar em intuição.
5. **Test** — validação JUnit automatizada.
6. **Trap** — anotar a pegadinha em `docs/traps/`.
7. **Question** — resolver questão no estilo da certificação.
8. **Review** — revisar o material acumulado (traps, cheat-sheets, error-log).
9. **Error Log** — registrar erros cometidos em `docs/error-log.md`.

## Toolchain

- Versões por **mise** (`mise.toml`): `java 21.0.2`, `maven 3.9.16`. O config global do mise aponta para Java 17 — confirme com `mise x -- java -version`.
- Testes: `mise x -- mvn test`. Teste isolado: `mise x -- mvn test -Dtest=SmokeTest`.
- Só **JUnit 5** (5.11.4); build com `maven.compiler.release=21`.
- Layout: código em `src/main/java/com/placido/certification/<tópico>/`, testes em `src/test/java/...`. Os diretórios de tópico no topo guardam notas/exemplos e **não** são fontes do Maven.

## Manutenção

- Exemplos devem ser pequenos e executáveis.
- Todo código novo deve compilar e ter teste; após qualquer alteração de código, rodar `mise x -- mvn test`.
- Não adicionar dependências nem transformar o projeto em multi-módulo sem confirmar.
- `target/` e `*.class` estão no `.gitignore`.

## Progresso e evidência

- **`docs/progress.yaml` é a fonte de verdade do progresso.** Ciclo: `planned` → `studying` → `review` → `done`.
- **Conteúdo gerado não significa domínio.** Um tópico só vira `done` com **evidência**: exercícios/testes próprios resolvidos e desempenho nas questões/mocks.
- Erros do estudante vão para `docs/error-log.md`; pegadinhas para `docs/traps/`.
- O exame 1Z0-830 **não cobre JDBC nem Reflection** (mantidos por relevância profissional). **Localization** é objetivo oficial e fica sob `datetime/`.
- Profundidade obrigatória: records, sealed types, pattern matching (`instanceof`/`switch`), virtual threads, sequenced collections, text blocks, try-with-resources, generics/wildcards, JPMS, streams paralelos.

## Mock exams

- `exams/` deve simular **questões de certificação** (múltipla escolha, estilo 1Z0-830).
- **Não inventar scores**: notas entram em `docs/progress.yaml` apenas de provas efetivamente resolvidas.

## Segurança operacional

- **Não apagar material existente** (arquivos, conteúdo, histórico) sem autorização explícita.
- **Não executar comandos destrutivos do Git** (`reset --hard`, `clean -fdx`, `push --force`, etc.) sem autorização explícita.