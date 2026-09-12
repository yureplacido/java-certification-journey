# Trap: LocalDate.plusYears após 29/02 retrocede ao último dia válido do mês

**Classificação:** Java API / Runtime
**Verificada:** 1Z0-830 diagnostic, Q07 (javac 21.0.2 + JVM real)

## Regra

- `LocalDate.plusYears(n)` — quando o dia do mês não existe no ano-alvo (ex.:
  29/02 num ano não bissexto), a JSR-310 resolve com `resolvePreviousValid`: o dia
  **retrocede** para o último dia válido do mês (`28` para fevereiro). Não lança
  exceção e não rola para o mês seguinte.
- `isLeapYear()`: divisível por 4 e (não divisível por 100 ou divisível por 400).

## Pegadinha

Achar que `2024-02-29.plusYears(1)` dá `2025-03-01` (o "dia extra sobra") ou que
lança `DateTimeException`. O correto é `2025-02-28`.

## Exemplo verificado

```java
LocalDate d = LocalDate.of(2024, 2, 29);
System.out.println(d.isLeapYear());              // true
System.out.println(d.plusYears(1));              // 2025-02-28
System.out.println(d.plusYears(4));              // 2028-02-29 (volta a ser válido)
```

## Dica de exame

- A regra vale para qualquer operação que mude o ano/mês e comprometa o dia
  (`plusMonths`, `withYear`, `Period.plus`), não só `plusYears`.
- `Period.between(jan/31, fev/29)` = `P29D` (0 meses), não `P1M` — a mesma
  filosofia: períodos respeitam dias válidos por mês.