# Trap: cause vs suppressed exceptions

**Classificação:** Java API / Runtime
**Verificada:** 1Z0-830 diagnostic, Q28 (javac 21.0.2 + JVM real)

## Regra

- O **cause** de uma exceção é o `Throwable` passado no construtor
  (`new RuntimeException("msg", cause)`) e aparece no stack trace como
  `Caused by: ...`.
- **Suppressed** são exceções anexadas **somente** pelo mecanismo de
  try-with-resources (JLS §14.20.3): quando o corpo lança uma exceção e o
  `close()` também lança, o `close()` vai para a lista de suppressed da
  exceção original via `Throwable.addSuppressed()`.

## Pegadinha

O exame costuma confundir os dois: achar que `new Ex("msg", e)` anexa `e`
como *suppressed*. Não — `e` vira *cause*. Sem try-with-resources, não existe
suppressed.

## Exemplo verificado

```java
try {
    throw new RuntimeException("Original");
} catch (RuntimeException e) {
    throw new RuntimeException("New", e);   // e vira CAUSE
} finally {
    System.out.println("Finally");
}
```

Saída real: `Finally` + stack trace de `New` com
`Caused by: java.lang.RuntimeException: Original`.

## Dica de exame

- `Throwable.getCause()` / construtor com `Throwable` → *cause*.
- `Throwable.getSuppressed()` / `addSuppressed()` → só try-with-resources.