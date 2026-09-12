# LocalDate — plusYears e datas inválidas após ajuste de ano

**Tópico:** datetime (1Z0-830 Seção 2 — Date-Time API)
**Classificação:** Java API / Runtime
**Origem:** revisão do diagnóstico 1Z0-830 — Q07 (gabarito A)
**Evidência:** `DiagnosticWeek2ReviewTest.q07_plusYearsAfterLeapDayRollsBackToFeb28` (JVM 21.0.2);
trap `docs/traps/localdate-plusyears-leap-rollback.md`; error-log 2026-09-12

## Regras

- `LocalDate.plusYears(long)` soma anos preservando dia e mês quando o dia é
  válido no ano-alvo.
- Quando o dia não existe no ano-alvo (ex.: 29/02 em ano não bissexto), a JSR-310
  resolve com `resolvePreviousValid`: o dia **retrocede para o último dia válido
  do mês**. **Não lança exceção** nesse caso e **não avança para o mês seguinte**.

## Exemplos verificados (Q07)

```java
LocalDate d = LocalDate.of(2024, 2, 29);
d.isLeapYear();          // true  (2024 é bissexto)
d.plusYears(1);          // 2025-02-28  (2025 não é bissexto: 29 -> último dia válido)
d.plusYears(4);          // 2028-02-29  (2028 é bissexto: dia 29 preservado)
```

- `isLeapYear()`: divisível por 4 e (não divisível por 100 **ou** divisível por 400).

## Pegadinha de exame

Esperar `2025-03-01` (o dia "extra" sobrar) ou esperar `DateTimeException`. A API
ajusta para dentro do mês, para a última data válida.