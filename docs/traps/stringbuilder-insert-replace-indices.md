# Trap: StringBuilder — insert desloca à direita e replace usa índice half-open

**Classificação:** Java API / Runtime
**Verificada:** 1Z0-830 diagnostic, Q05 (javac 21.0.2 + JVM real)

## Regra

- `StringBuilder.insert(int offset, String s)` insere o texto **antes** do índice
  `offset`, empurrando o conteúdo restante para a direita.
- `StringBuilder.replace(int start, int end, String s)` substitui o intervalo
  `[start, end)` — o **end é exclusivo** — pelo texto `s`. É diferente de
  `String.replace(CharSequence, CharSequence)`, que substitui **texto literal**
  (todas as ocorrências), sem noção de índice.

## Pegadinha

- Achar que `replace(1, 3, "a")` em `"Hellolo"` troca "Hel" por "a". Não: troca
  os chars dos índices 1 e 2 (`el`) → `"Halolo"`.
- Esquecer de recalcular a string depois de um `insert` antes de aplicar o próximo
  comando baseado em índices.

## Exemplo verificado

```java
StringBuilder sb = new StringBuilder("Hello"); // H e l l o
sb.insert(3, "lo");                            // H e l l o l o  ("Hellolo")
sb.replace(1, 3, "a");                         // substitui [1,3): "e"+"l" -> "a"
System.out.println(sb);                        // "Halolo"
```

## Dica de exame

- Sempre reescrever a string no papel após cada operação mutável e recontar índices.
- Lembrete de tipos: `StringBuilder` (não sincronizado) vs `StringBuffer`
  (sincronizado) — mesma API de índices nos dois.