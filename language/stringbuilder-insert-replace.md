# StringBuilder — insert e replace com índices

**Tópico:** language (1Z0-830 Seção 1 — Language Basics)
**Classificação:** Java API / Runtime
**Origem:** revisão do diagnóstico 1Z0-830 — Q05 (gabarito B)
**Evidência:** `DiagnosticWeek2ReviewTest.q05_stringBuilderInsertShiftsThenReplaceIsHalfOpen` (JVM 21.0.2);
trap `docs/traps/stringbuilder-insert-replace-indices.md`; error-log 2026-09-12

## Regras

- `insert(int index, String value)` — insere `value` **antes** do char no índice
  `index`; o conteúdo restante é empurrado para a direita. `index` é inclusivo
  (válido de `0` até `length()`).
- `replace(int start, int end, String value)` — substitui o intervalo
  `[start, end)` — **start inclusivo, end exclusivo** — pelo texto `value`.
- Os índices são avaliados sobre o **estado atual** do `StringBuilder`: operações
  anteriores (`insert`/`delete`/`append`) reposicionam os caracteres para as
  operações seguintes.
- `StringBuilder.replace` usa índices de char — **não** é a substituição de
  texto literal de `String.replace(CharSequence, CharSequence)`.

## Exemplo verificado (Q05)

```java
StringBuilder sb = new StringBuilder("Hello");
sb.insert(3, "lo");     // "Hellolo"  (insere antes do índice 3)
sb.replace(1, 3, "a");  // [1,3) -> "el" vira "a"
System.out.println(sb); // "Halolo"
```

Passo a passo:

| Operação | Conteúdo | Índices (0-based) |
| --- | --- | --- |
| `new StringBuilder("Hello")` | `H e l l o` | `0 1 2 3 4` |
| `insert(3, "lo")` | `H e l l o l o` | `0 1 2 3 4 5 6` |
| `replace(1, 3, "a")` → `[1,3)` | `H a l o l o` | `0 1 2 3 4 5` |

## Pegadinha de exame

Aplicar o `replace` sobre os índices da string original depois de um `insert` e
esquecer o end exclusivo: `replace(1, 3, ...)` não toca o índice 3.