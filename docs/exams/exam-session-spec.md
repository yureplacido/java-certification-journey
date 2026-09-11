# Especificação do Modelo de Exam Session

**Status:** v1 — aprovado para implementação dos comandos de sessão
**Alvo:** exame Oracle Certified Professional: Java SE 21 Developer (1Z0-830)
**Baseline:** Java 21 (fase 1Z0-830)
**Local:** `exams/sessions/` (persistência) · **Definições:** `exams/<examId>/`

---

## 1. Propósito

Formalizar a distinção entre **Exam Definition** (a prova em si, imutável) e
**Exam Session** (uma execução específica dessa prova), e definir como uma
sessão é persistida no próprio repositório — sem banco de dados, sem o LLM
como fonte de estado.

O repositório Git é a **fonte de verdade**: o estado de uma sessão vive em um
arquivo YAML versionável, e qualquer retomada converte esse arquivo no estado
corrente de execução.

### O que esta especificação NÃO cobre

- Geração de questões/provas (ver `.opencode/commands/mock-exam.md`).
- Implementação dos comandos do OpenCode (`start`, `resume`, `pause`,
  `finish`, `grade`…). Este documento é o contrato que esses comandos devem
  respeitar posteriormente.
- Conteúdo pedagógico (notas, traps, cheat-sheets).

---

## 2. Entidades

### 2.1 Exam Definition (imutável)

Representa a definição de um simulado: pergunta, gabarito e metadados de
correção. **Não muda durante a vigência de uma sessão.** Qualquer alteração
produz uma **nova versão** (`version` incrementada → nova definição).

Instância canônica: `exams/<examId>/`

```
exams/mock-01/
├── exam.md            # descrição humana (instruções, formato)
├── definition.yaml    # metadados + ordem + metadados por questão + gabarito
└── questions/
    ├── Q01.md         # id, enunciado, opções, explicação, frontmatter tópico/dificuldade
    └── ...
```

Campos mínimos de `definition.yaml`:

| Campo | Tipo | Descrição |
| --- | --- | --- |
| `examId` | string | identificador (ex.: `mock-01`) |
| `version` | int | versão da definição (imutável dentro da sessão) |
| `examType` | enum | `diagnostic` \| `mock` \| `final` |
| `javaVersion` | string | baseline (ex.: `"21"`) |
| `totalQuestions` | int | quantidade de questões |
| `durationSeconds` | int | duração da prova (ex.: 120 min = 7200) |
| `passingScore` | int | % mínima para aprovação (ex.: 68) |
| `questionOrder` | list | ordem das questões (lista de `questionId`) |
| `questions` | map | `questionId → { topic, section, difficulty }` |
| `answers` | map | `questionId → { correctOption, format }` — o gabarito |

**Importante:** o gabarito (`answers`) e os metadados (`questions`) são o que
permite a correção. Uma sessão nunca carrega esses dados — só referencia a
definição por `examId` + `examVersion`.

**Identificador canônico de questão:** todo `questionId` usa o formato
`Q01`…`Q50` (zero-padded). `questionOrder`, `questions` (tanto na definição
quanto na sessão) e `currentQuestion` referenciam **o mesmo** identificador
canônico — não existe convenção numérica separada.

> Layout atual: o `exams/diagnostic/` usa `README.md`/`questions.md`/
> `answersheet.md` (validado, em uso). O layout canônico acima vale para novos
> simulados; a migração do diagnostic para o layout canônico, se ocorrer, é
> mudança estrutural e exige autorização.

### 2.2 Exam Session

Representa **uma execução** de uma Exam Definition. Uma mesma definição pode
ter **N sessões** ao longo do tempo, mas apenas **uma ativa** por
(`examId`, `examVersion`) — ver §6.

Instância: `exams/sessions/<sessionId>.yaml`

### 2.3 Estado por questão

O estado da sessão é formado pela agregação de estados por questão:

| Estado | Semântica |
| --- | --- |
| `NOT_VISITED` | ainda não alcançada (default implícito) |
| `VISITED` | exibida, sem resposta registrada |
| `ANSWERED` | resposta registrada (`answer`) |
| `SKIPPED` | pular explicitamente (sem resposta) |

`flagged` (bool) é **ortogonal** ao status: uma questão pode ser marcada
estando `ANSWERED`, `VISITED` ou `SKIPPED`.

Transições de questão (válidas):

```
NOT_VISITED ──▶ VISITED ──▶ ANSWERED
                   │   ▲       ▲
                   ▼   │       │
                  SKIPPED ─────┘
```

Regras:
- `VISITED → ANSWERED` **registra** uma resposta (preenche `answer`).
- `SKIPPED → ANSWERED` **registra** uma resposta (voltar e responder).
- `ANSWERED → VISITED` **remove a resposta vigente** (o campo `answer` é
  removido) — é o único mecanismo de remoção de resposta.
- `ANSWERED → SKIPPED` **não é usado como mecanismo de remoção de resposta**;
  para remover, passa-se por `ANSWERED → VISITED`. Uma questão só fica
  `SKIPPED` sem resposta registrada (ou após retirar a resposta, se optou por
  pular depois).
- Histórico anterior de respostas (ex.: `answersHistory`) **não** faz parte do
  modelo atual; se um dia for necessário preservar a noção de que uma questão
  já foi pulada/respondida, é evolução futura aditiva — fora desta
  especificação. A fonte de verdade é sempre a resposta vigente no momento da
  finalização.

---

## 3. Estados da sessão

Enum formal:

| Estado | Significado |
| --- | --- |
| `NOT_STARTED` | sessão criada/agendada, prova não iniciada |
| `IN_PROGRESS` | prova em andamento (seleção ativa) |
| `PAUSED` | interrompida, continuará do ponto salvo |
| `FINISHED` | prova concluída/enviada; correção ainda não feita |
| `GRADED` | respostas conferidas contra o gabarito; resultado derivado persistido |
| `ANALYZED` | resultado analisado (lacunas/reflexos em `docs/` derivados de dados reais) |
| `ABANDONED` | abandonada (não gera correção) |

---

## 4. Máquina de estados

```
                    start
       NOT_STARTED ────────▶ IN_PROGRESS
                                 │   │
                          pause  │   │ resume
                                 ▼   │
                               PAUSED ┘
                                 │  │
                          resume │  │ abandon
                                 │  ▼
                    submit       │ ABANDONED (terminal)
       IN_PROGRESS ──────────────┘
              │
              ▼
          FINISHED ──grade──▶ GRADED ──analyze──▶ ANALYZED (terminal)
```

### Transições válidas

| De | Para | Ação |
| --- | --- | --- |
| `NOT_STARTED` | `IN_PROGRESS` | `start` |
| `IN_PROGRESS` | `PAUSED` | `pause` |
| `PAUSED` | `IN_PROGRESS` | `resume` |
| `IN_PROGRESS` | `FINISHED` | `finish` (envio manual ou tempo esgotado) |
| `IN_PROGRESS` | `ABANDONED` | `abandon` |
| `PAUSED` | `ABANDONED` | `abandon` |
| `FINISHED` | `GRADED` | `grade` (automático/idempotente) |
| `GRADED` | `ANALYZED` | `analyze` |

### Transições INVÁLIDAS (proibidas)

| De | Para | Por quê |
| --- | --- | --- |
| `PAUSED` | `FINISHED` | finalização exige estar ativa (`IN_PROGRESS`); pausada precisa `resume` antes |
| `FINISHED` | `IN_PROGRESS` | sessão finalizada é imutável; retorno exige nova sessão |
| `FINISHED` | `ABANDONED` | prova enviada é final; se não houver correção, fica `FINISHED` |
| `GRADED` | `FINISHED` | resultado derivado não é revertível na mesma sessão |
| `NOT_STARTED` | `PAUSED` / `FINISHED` / `GRADED` / `ANALYZED` / `ABANDONED` | só sai por `start` |
| `PAUSED` | `GRADED` / `ANALYZED` | correção exige `FINISHED` |
| `ABANDONED` / `ANALYZED` | qualquer | terminais |

---

## 5. Estrutura de persistência

### 5.1 Local e nome de arquivo

- Diretório único: **`exams/sessions/`**
- Um arquivo por sessão: **`exams/sessions/<sessionId>.yaml`**
- `sessionId` = `<YYYY-MM-DD>-<examId>-<NN>` (ex.: `2026-09-10-mock-03-01`),
  `NN` = sequência zero-padded única no repositório. Se houver colisão,
  incrementa `NN`.

### 5.2 Campos da sessão

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `schemaVersion` | int | sim | versão do esquema (hoje `1`) para evolução futura |
| `sessionId` | string | sim | identidade (ver §5.1) |
| `examId` | string | sim | referencia a definição |
| `examVersion` | int | sim | versão da definição usada nesta sessão (imutável) |
| `status` | enum | sim | estado corrente (ver §3) |
| `startedAt` | timestamp (ISO-8601, com offset) | sim | início da sessão |
| `lastActivityAt` | timestamp | sim | última alteração (pause/finish/answer) |
| `finishedAt` | timestamp | não | preenchido no `finish` |
| `currentQuestion` | string (`questionId`) | sim | questão ativa na última atividade (a reexibir no resume); usa o identificador canônico `Q01`…`Q50` |
| `elapsedSeconds` | int | sim | tempo decorrido **ativo** (não conta pausa) |
| `remainingSeconds` | int | sim | tempo restante = `durationSeconds − elapsedSeconds` |
| `totalQuestions` | int | sim | espelho da definição (auditoria) |
| `answeredQuestions` | int | sim | contador derivado (ver §8 invariantes) |
| `skippedQuestions` | int | sim | idem |
| `visitedQuestions` | int | sim | idem |
| `flaggedQuestions` | int | sim | idem |
| `result` | map | não | preenchido só no `grade` (§9) |
| `questions` | map | sim | `questionId → { status, answer?, flagged?, updatedAt? }` |

### 5.3 Campos por questão

```yaml
questions:
  Q07:
    status: VISITED            # NOT_VISITED | VISITED | ANSWERED | SKIPPED
    flagged: false             # opcional, default false
    updatedAt: "2026-09-10T14:52:00-03:00"   # opcional, auditoria
  Q09:
    status: ANSWERED
    answer: B                  # só presente em ANSWERED
```

Convenções:
- Questão sem entrada = `NOT_VISITED` (não é preciso listar as 50 sempre);
  o questionId implícito segue o mesmo canônico `Q01`…`Q50`.
- `flagged: false` pode ser omitido.
- `result` **não** é editado à mão: é derivado pelo `grade` (§9).

---

## 6. Relação Exam Definition ↔ Exam Session

- **1 : N** — uma definição pode originar muitas sessões
  (`session-01`, `session-02` de `mock-01`).
- **Imutabilidade da definição:** a sessão referencia `(examId,
  examVersion)`. Se a definição mudar, é outra versão; a sessão antiga não
  relê a nova.
- **Uma sessão ativa por versão:** pode existir no máximo uma sessão
  `IN_PROGRESS` **ou** `PAUSED` para o mesmo `(examId, examVersion)`. Para
  criar uma segunda sessão, a anterior deve estar `FINISHED`, `GRADED`,
  `ANALYZED` ou `ABANDONED`.
- **Versão nova ≠ sessão nova:** se a definição subir de versão, a sessão
  ativa da versão antiga continua sendo dela. Uma nova sessão pode ser criada
  para a versão nova **se** não existir ativa para *essa* versão.

---

## 7. Regras de retomada (resume)

1. `resume` procura, para o `(examId, examVersion)` pedido, a sessão com
   `status ∈ {IN_PROGRESS, PAUSED}`. Encontrando, **reabre essa sessão** — não
   cria outra (regra 3 do objetivo).
2. Se não existir sessão ativa e o examinando quiser outra tentativa:
   cria-se **nova** sessão (regra 4).
3. Na retomada, o estado corrente é reconstruído **integralmente do arquivo**:
   `currentQuestion`, `elapsedSeconds`/`remainingSeconds`, contadores e todas
   as respostas por questão (§5). Nada é recalculado por intuição ou solicitado
   ao LLM.
4. O relógio **recomeça a contar a partir de `remainingSeconds`** no instante da
   retomada; o tempo em `PAUSED` não conta.
   `remainingSeconds_após_resume = remainingSeconds_persistido − (agora − lastActivityAt)`.
   Se `remainingSeconds ≤ 0` na retomada → a sessão é tratada como expirada e
   vai a `FINISHED` (a resposta já registrada é preservada).
5. Se o `examVersion` registrado divergir do corrente da definição, o resume
   **mantém a versão da sessão** (e informa a divergência); não re-associa a
   sessão a outra definição.

---

## 8. Regras de finalização (finish)

- A sessão precisa estar `IN_PROGRESS` (pausada exige `resume` primeiro).
- `finish` ocorre por: envio manual, ou tempo esgotado
  (`remainingSeconds ≤ 0`).
- Efeitos: `status = FINISHED`, `finishedAt = agora`,
  `remainingSeconds = max(0, remainingSeconds)`.
- As respostas registradas **não mudam**: o que está no arquivo no momento do
  envio é a prova final. Questões `VISITED`/`SKIPPED`/`NOT_VISITED` não viram
  resposta automaticamente.
- `FINISHED` é imutável: nenhuma resposta pode ser alterada depois.

### Invariantes verificáveis (a qualquer momento)

```
answeredQuestions == contagem(status == ANSWERED)
skippedQuestions  == contagem(status == SKIPPED)
visitedQuestions  == contagem(status != NOT_VISITED)
flaggedQuestions  == contagem(flagged == true)
answered + skipped + visited-pendentes + not-visited == totalQuestions
currentQuestion é um questionId válido (∈ questionOrder da definição)
0 <= remainingSeconds <= durationSeconds
elapsedSeconds + remainingSeconds == durationSeconds
```

Se uma invariante falhar, o arquivo está inválido: **não** inventar valores;
sinalizar e (com autorização) reconstruir apenas o que for derivável, nunca
respostas.

---

## 9. Regras de correção (grade)

1. Correção **só** roda sobre `FINISHED`. Produz `GRADED`.
2. Resultado é **derivado** das respostas efetivamente registradas no arquivo,
   comparadas com o gabarito de `definition.yaml` (`answers`), na ordem da
   `questionOrder` da definição.
3. O **resultado acadêmico** (`correct`, `wrong`, `unanswered`,
   `scorePercent`, `passing`, `bySection`) é **determinístico**: para a mesma
   (Exam Definition, Exam Session) ele é sempre o mesmo, pois deriva só das
   respostas registradas e do gabarito. O grade é também **idempotente**:
   reprocessar uma sessão já `GRADED` não altera o resultado acadêmico.
   `gradedAt` é **metadata temporal** — atualizada quando o grade é
   reprocessado — e **não** faz parte do resultado determinístico.
4. Bloco `result` gravado na sessão durante o `grade`:

```yaml
result:
  gradedAt: "2026-09-10T15:10:00-03:00"   # metadata temporal (atualizável no reprocessamento)
  correct: 34                              # ↓ resultado acadêmico determinístico
  wrong: 12
  unanswered: 4
  scorePercent: 68
  passing: true
  bySection:
    1: { correct: 5, total: 6 }
    2: { correct: 2, total: 4 }
```

> **Definição de `scorePercent`:** `round(100 × correct / totalQuestions)`.
> Questão respondida com opção inválida conta como **errada** (`wrong`), não
> como não respondida — o registro de uma resposta errada é uma resposta.
> Questão `ANSWERED` sem `answer` (corrupção) é tratada como não respondida.

5. `analyze` (estado `ANALYZED`) consome o `result` real e deriva lacunas
   (seção, tópico, tipo de pegadinha) — registradas em `docs/`, nunca
   inventadas.
6. `docs/progress.yaml` **só** recebe desempenho vindo de sessões reais
   `GRADED`/`ANALYZED` (regras 9 e 10 do objetivo). Nota digitada à mão é
   proibida.

---

## 10. Exemplo completo: sessão interrompida na Q30 (prova de 50)

Prova hipotética: `mock-03`, versão 1, 50 questões, 120 min (7200 s).

**Definição (conceitual, trecho de `definition.yaml` do futuro):**

```yaml
examId: mock-03
version: 1
examType: mock
javaVersion: "21"
totalQuestions: 50
durationSeconds: 7200
passingScore: 68
questionOrder: [Q01, Q02, ..., Q50]
questions:
  Q01: { topic: language-basics, section: 1, difficulty: medium }
  ...
answers:
  Q01: { correctOption: B, format: single-choice }
  Q02: { correctOption: D, format: single-choice }
  ...
```

**Sessão salva** (`exams/sessions/2026-09-10-mock-03-01.yaml`) — arquivo real
de exemplo em `exams/sessions/session-example.yaml` (§11). Resumo do estado
persistido:

```yaml
sessionId: "2026-09-10-mock-03-01"
examId: mock-03
examVersion: 1
status: PAUSED
currentQuestion: Q30
elapsedSeconds: 2100
remainingSeconds: 5100
totalQuestions: 50
answeredQuestions: 16
skippedQuestions: 2
visitedQuestions: 30
flaggedQuestions: 4
questions:
  Q01: { status: ANSWERED, answer: B }
  Q02: { status: ANSWERED, answer: D }
  Q03: { status: SKIPPED }
  ...
  Q30: { status: VISITED, flagged: true }
  # Q31..Q50: implícitas como NOT_VISITED
```

Retomada reproduz: questão Q30 em tela, cronômetro em 85:00,
respostas Q01..Q30 preservadas, contadores consistentes.

---

## 11. Arquivo de exemplo

`exams/sessions/session-example.yaml` — **sessão sintética** criada para
validar o modelo persistido (status `PAUSED`, interrompida na Q30 de 50).
Não representa progresso real, não alimenta `docs/progress.yaml`, e referencia
uma definição `mock-03` que ainda não existe.

---

## 12. Operação de resposta (ExamSessionAnswerer)

Esta seção define o contrato da operação pública de registro de resposta
individual em uma sessão de exame.

### 12.1 Propósito e assinatura

```
ExamSessionAnswerer.answer(sessionId, questionId, answer) → AnswerResult
```

Registra a resposta de uma questão na sessão, atualizando o estado da questão
para `ANSWERED`, preserva a resposta anterior apenas como substituição, e
deriva os contadores do estado resultante. **Não** registra `VISITED`/`SKIPPED`/
`flagged` — esses são operações fora deste contrato (futuras).

### 12.2 Pré-condições (ordem de validação)

| Condição | Falha => erro |
| --- | --- |
| Sessão existe (`exams/sessions/<sessionId>.yaml`) | `SESSION_NOT_FOUND` |
| Sessão está `IN_PROGRESS` | `SESSION_NOT_ANSWERABLE` |
| Exam Definition existe e `version` casa com `examVersion` da sessão | `EXAM_DEFINITION_NOT_FOUND` / `EXAM_DEFINITION_VERSION_MISMATCH` |
| `questionId` ∈ `questionOrder` da definição | `QUESTION_NOT_IN_EXAM` |
| `answer` é uma opção permitida para a questão | `INVALID_ANSWER_OPTION` |

> Estados `PAUSED`, `FINISHED`, `GRADED`, `ANALYZED` e `ABANDONED` **não** são
> respondíveis. Sessão pausada exige `resume` antes de responder (§7).

### 12.3 Validação da resposta contra as opções

- Se a entrada `answers['<questionId>']` da definição declarar uma lista
  `options`, a resposta deve pertencer a essa lista.
- Caso contrário, o **fallback é `{A, B, C, D}`** para `format: single-choice`
  (o formato do exame 1Z0-830). Esse fallback é implícito e determinístico.
- A comparação é **sensível a maiúsculas** (`B` ≠ `b`).

### 12.4 Transição de estado da questão

A questão indexada por `questionId` recebe:

```yaml
<questionId>:
  status: ANSWERED
  answer: <answer>            # já normalizada como string
  flagged: <preservado>       # se já existia; não é criado/removido
  updatedAt: <now>            # timestamp da operação
```

- Resposta registrada em questão `NOT_VISITED`, `VISITED` ou `SKIPPED` é uma
  **primeira resposta** (outcome `ANSWERED`).
- Questão já `ANSWERED` aceita **substituição** da resposta (outcome `UPDATED`)
  e resposta idêntica é **idempotente** (outcome `UNCHANGED`).
- `flagged` é **ortogonal ao status** (§2.3) e é preservado tal qual estava.

### 12.5 Efeitos em contadores, tempo e navegação

- Contadores são **derivados** do novo estado (nunca incrementados cegamente):
  - `answeredQuestions` = nº de `status == ANSWERED`
  - `skippedQuestions`  = nº de `status == SKIPPED`
  - `visitedQuestions`  = nº de `status != NOT_VISITED`
  - `flaggedQuestions`  = nº de `flagged == true`
- **Tempo:** consome-se tempo ativo desde `lastActivityAt` até `now` usando o
  mesmo cálculo de `SessionTime` (resume/pause/finish); `elapsedSeconds`/
  `remainingSeconds` atualizados e `lastActivityAt = now`. Registrar resposta
  com `remainingSeconds == 0` é permitido (a expiração é transição de
  `resume`/`pause`/`finish`, fora deste contrato).
- `currentQuestion` **não muda** — preservada como estava.

### 12.6 Persistência

Escrita **atômica e segura**: grava em arquivo temporário e move com
`ATOMIC_MOVE` (fallback para `REPLACE_EXISTING`), mesmo padrão usado pelo
`Progress`. Erros de escrita => `SESSION_STORE_UNREADABLE`.

### 12.7 Determinismo

Para as mesmas entradas (`sessionId`, `questionId`, `answer`) e o mesmo
relógio, a operação produz o mesmo estado persistido e o mesmo `AnswerResult`.
Não há fontes de não-determinismo (ordem de iteração, hash, etc.) no resultado
acadêmico da sessão.

### 12.8 Invariantes verificáveis após a operação (herdadas da §8)

```
answeredQuestions == contagem(status == ANSWERED)
skippedQuestions  == contagem(status == SKIPPED)
visitedQuestions  == contagem(status != NOT_VISITED)
flaggedQuestions  == contagem(flagged == true)
elapsedSeconds + remainingSeconds == durationSeconds
currentQuestion é um questionId canônico
```

---

## 13. Notas de implementação futura (fora deste escopo)

- Comandos do OpenCode (`exam start|resume|pause|finish|grade|abandon|answer`)
  devem apenas **transicionar estados validando a tabela da §4** e atualizar o
  arquivo; nunca inferir conteúdo.
- `visit`, `skip` e `flag` (transições de questão da §2.3) ainda **não** têm
  operação implementada; `ExamSessionAnswerer` cobre apenas a resposta.
- Um `validate` pode checar invariantes (§8) de todas as sessões sem tocar em
  conteúdo.