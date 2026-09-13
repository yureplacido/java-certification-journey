# Error Log

Registro cumulativo de erros cometidos durante o estudo (compilação, lógica, comportamento de API,
pegadinhas de exame). É material de revisão pré-exame — revisitar as entradas antigas é parte do estudo.

## Como registrar

Para cada erro significativo, criar uma entrada:

```markdown
## <data> — <tópico> — <título curto>

- **Contexto:** o que estava fazendo / código envolvido.
- **Erro / sintoma:** o que aconteceu (mensagem de erro, saída errada, exceção).
- **Causa raiz:** por que aconteceu (regra da linguagem/API violada).
- **Lição:** a regra fixada para não repetir o erro.
```

## Entradas

**_Registros começarão durante o estudo (semana 1 em diante)._**

## 2026-09-13 — language (StringBuilder) — Prática: 2/6 exercícios errados de cabeça

- **Contexto:** exercícios de recálculo de índices após mutações (stringbuilder-practice.md).
  Ex3 e Ex6 errados na resolução "de cabeça"; ambos validados por execução.
- **Erro / sintoma:**
  1. Ex3: respondi `XaYa`; o correto é `XAYa` (original `"Aa"`; `replace(2,2,"Y")` insere antes do `a` minúsculo).
  2. Ex6: respondi `How's going up?`; o correto é `How's goings up?` — após `insert(5, " going")` a string é `"What' goings up?"` e o `s` final de `What's` permanece.
- **Causa raiz:** reescrita parcial da string no papel — mantive os caracteres não tocados por memória visual em vez de reconstruir char a char. No Ex6, o recálculo bombeou os índices mas ignorei o `s` remanescente.
- **Lição:** reescrever a **string inteira** (não só o trecho mutado) após cada operação e conferir que todos os caracteres foram preservados com caixa e conteúdo corretos. Regra idêntica à do Q05: o estado do StringBuilder é a fonte de verdade.

## 2026-09-13 — language (StringBuilder) — Experimentos: 3 suposições falsas sobre replace/insert

- **Contexto:** experimentos JUnit (StringBuilderInsertReplaceTest) para validar casos de
  borda além do Q05.
- **Erro / sintoma:** 3 de 10 experimentos falharam na primeira versão:
  1. assumi que `replace(0, 4, "x")` em `"abc"` lança exceção (end > length) — na JVM real **não lança**: end é truncado.
  2. assumi que `insert(1, (String) null)` lança NPE — na JVM real insere o texto `"null"`.
  3. calculei `"banana".replace(1, 3, "or")` como `borona` — o correto é `borana` (índices 1,2 = "an").
- **Causa raiz:** extrapolei a regra conhecida (end exclusivo) para o caso `end > length`
  sem verificar; e confundi a semântica de overloads de `insert` (não confundir com o NPE de
  `String.replace`/append de objeto nulo).
- **Lição:** confiar no experimento, não na intuição. Regras fixadas (JVM 21.0.2):
  - `replace(start, end, s)`: `end` acima de `length()` é **truncado**, não lança; `start` inválido lança.
  - `insert(pos, (String) null)` insere o literal `"null"` (NÃO lança NPE).
  - refazer o cálculo char a char nos índices antes de afirmar a string resultante.

## 2026-09-12 — language (StringBuilder) — Diagnostic Q05: insert + replace com índices

- **Contexto:** questão Q05 do diagnóstico — `new StringBuilder("Hello")`, depois
  `sb.insert(3, "lo")` e `sb.replace(1, 3, "a")`; marquei A (`Helloo`), gabarito B (`Halolo`).
- **Erro / sintoma:** não recalculei a string após o `insert` antes de aplicar o `replace`,
  nem considerei que `replace(start, end, str)` tem end **exclusivo**.
- **Causa raiz:** misturei a semântica de `String.replace` (substituição literal de texto)
  com a de `StringBuilder.replace` (índices de char, intervalo half-open `[start, end)`);
  e neguei o deslocamento à direita causado pelo `insert`.
- **Lição:** em `StringBuilder`, `insert(pos, s)` empurra o conteúdo à direita; `replace(s, e, s)`
  substitui `[s, e)` (e exclusivo). Sempre recontar índices depois de cada operação mutável.

## 2026-09-12 — datetime (LocalDate) — Diagnostic Q07: plusYears após 29/02

- **Contexto:** questão Q07 — `LocalDate.of(2024, 2, 29).plusYears(1)`; marquei B
  (`true 2025-03-01`), gabarito A (`true 2025-02-28`).
- **Erro / sintoma:** esperei que o dia extra "sobrasse" e rolasse para 01/03.
- **Causa raiz:** desconhecia a regra de resolução da JSR-310 — `plusYears` usa
  `resolvePreviousValid`: se 29/02 não existe no ano-alvo, o dia retrocede para o último
  dia válido do mês (28/02). Não lança exceção nem muda de mês.
- **Lição:** operações que somam unidades a `LocalDate`/`LocalTime` ajustam o campo do dia ao
  menor valor válido do mês-alvo quando o dia original não existe (confirmado por experimento:
  `2024-02-29.plusYears(4)` retorna `2028-02-29`).

## 2026-09-12 — datetime (ZonedDateTime) — Diagnostic Q09: conversão com DST e instante

- **Contexto:** questão Q09 — `ZonedDateTime.of(2024, 7, 15, 10, 30, ..., America/New_York)`
  com `withZoneSameInstant(Asia/Tokyo)`; deixei em branco, gabarito D (`23 15`).
- **Erro / sintoma:** lacuna total — não computei a conversão de fuso com DST ativo.
- **Causa raiz:** não sabia que em julho NY está em EDT (UTC−4), não EST (UTC−5); e não
  conhecia a semântica de `withZoneSameInstant` (preserva o instante, muda a hora local
  conforme o offset da zona-alvo). Tokyo em julho: JST (UTC+9) → 10:30 − 4h + 9h = 23:30, mesmo dia.
- **Lição:** conferir o offset efetivo da zona na data (DST!) e lembrar que
  `withZoneSameInstant` mantém o **mesmo instante** — prova: `Duration.between(zdt, convertido)`
  é `PT0S`; as horas de diferença são dos offsets, não dos instantes.