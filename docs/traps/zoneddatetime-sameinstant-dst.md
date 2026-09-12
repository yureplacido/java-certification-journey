# Trap: withZoneSameInstant preserva o instante — DST muda o offset da zona

**Classificação:** Java API / Runtime
**Verificada:** 1Z0-830 diagnostic, Q09 (javac 21.0.2 + JVM real)

## Regra

- `ZonedDateTime.withZoneSameInstant(ZoneId)` converte para outra zona **mantendo o
  instante**: hora local = instante ajustado ao offset da zona-alvo naquela data.
- `withZoneSameLocal(ZoneId)` apenas troca a zona mantendo a hora local (outro
  instante) — a pegadinha oposta.
- O offset de uma zona varia com DST: Nova York em julho é **EDT (UTC−4)**; em
  janeiro, **EST (UTC−5)**. Tóquio (JST) não tem DST: sempre UTC+9.

## Pegadinha

- Assumir EST (−5) para qualquer data em `America/New_York` — em julho o correto
  é EDT (−4): julho/2024, NY 10:30 → Tokyo 23:30 (13h de diferença de offset).
- Confundir a diferença entre os **offsets** (13h) com uma duração entre os
  temporais: os dois `ZonedDateTime` representam o mesmo instante, logo
  `Duration.between` entre eles é `PT0S`.

## Exemplo verificado

```java
var zdt = ZonedDateTime.of(2024, 7, 15, 10, 30, 0, 0, ZoneId.of("America/New_York"));
var conv = zdt.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));

System.out.println(zdt.getOffset());   // -04:00 (EDT)
System.out.println(conv);              // 2024-07-15T23:30+09:00[Asia/Tokyo]
System.out.println(Duration.between(zdt, conv)); // PT0S (mesmo instante)
```

## Dica de exame

- Preste atenção no **mês** do código: julho/agosto → horário de verão do
  hemisfério norte; apegue-se ao offset efetivo, nunca ao "offset da zona".
- Para acertar a conta: instante UTC = hora local − offset; depois hora-alvo =
  instante + offset da zona-alvo.