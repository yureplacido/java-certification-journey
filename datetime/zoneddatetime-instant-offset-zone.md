# ZonedDateTime — instant, offset, zone e withZoneSameInstant

**Tópico:** datetime (1Z0-830 Seção 2 — Date-Time API)
**Classificação:** Java API / Runtime
**Origem:** revisão do diagnóstico 1Z0-830 — Q09 (gabarito D)
**Evidência:** `DiagnosticWeek2ReviewTest.q09_withZoneSameInstantFromEdtToJst` (JVM 21.0.2);
trap `docs/traps/zoneddatetime-sameinstant-dst.md`; error-log 2026-09-12

## Conceitos

- **instant:** o ponto na linha do tempo, independente de zona/offset (`toInstant()`).
- **offset:** a diferença do fuso em relação a UTC naquele momento (ex.: `-04:00`).
- **zone:** a região (ex.: `America/New_York`); o offset dela **varia com DST**.

## Regras

- `withZoneSameInstant(ZoneId)` — converte para outra zona **preservando o
  instante**: o horário local muda conforme o offset da zona-alvo naquela data.
- `withZoneSameLocal(ZoneId)` — troca a zona **mantendo o horário local** (altera
  o instante) — é a pegadinha oposta.
- Após `withZoneSameInstant`, os dois temporais representam o **mesmo instante**:
  `Duration.between(zdt, converted)` é `PT0S`. A diferença de horário local vem
  dos offsets, não dos instantes.

## DST — exemplo verificado (Q09)

Em julho, `America/New_York` está em **EDT (UTC−4)** — não EST (−5).
`Asia/Tokyo` é **JST (UTC+9)**, sem DST. Diferença de horários locais: 13 horas.

```java
var zdt = ZonedDateTime.of(2024, 7, 15, 10, 30, 0, 0, ZoneId.of("America/New_York"));
var conv = zdt.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));

zdt.getOffset();       // -04:00 (EDT)
conv.getOffset();      // +09:00 (JST)
conv.getHour();        // 23
conv.getDayOfMonth();  // 15
zdt.toInstant().equals(conv.toInstant()); // true (mesmo instante)
Duration.between(zdt, conv);              // PT0S
```

Conta: `10:30 − (−4h) = 14:30 UTC`; `14:30 + 9h = 23:30 JST` — mesmo dia (15).

## Pegadinha de exame

Assumir o offset "fixo" da zona (EST = −5 para NY em qualquer data): o **mês** do
código determina se vale horário de verão.