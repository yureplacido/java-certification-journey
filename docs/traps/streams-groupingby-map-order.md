# Trap: groupingBy devolve HashMap — ordem de iteração não especificada

**Classificação:** Java API / Runtime
**Verificada:** 1Z0-830 diagnostic, Q41 (javac 21.0.2 + JVM real)

## Regra

- `Collectors.groupingBy(classifier)` retorna um `HashMap` (a menos que um
  supplier seja passado). A ordem de iteração de um `HashMap` **não é
  especificada** — pode coincidir com a ordem esperada em uma execução e
  mudar em outra.
- O overload de três argumentos `groupingBy(classifier, mapFactory,
  downstream)` permite escolher o mapa, ex.: `TreeMap::new` → chaves
  ordenadas.

## Pegadinha

Questão que afirma `{a=[...], b=[...], c=[...]}` para `groupingBy` padrão
depende de ordem não contratual: pode "passar" na sua JVM e falhar/divergir
em outra (ou em versões futuras do JDK). Em código de exame/preparação, usar
sempre o factory para garantir determinismo quando a ordem importa.

## Exemplo verificado

```java
var grouped = List.of("apple", "banana", "cherry", "avocado", "blueberry")
    .stream()
    .collect(Collectors.groupingBy(w -> w.charAt(0), TreeMap::new, Collectors.toList()));
System.out.println(grouped); // {a=[apple, avocado], b=[banana, blueberry], c=[cherry]}
```

## Dica de exame

- Default: `HashMap` → ordem não garantida.
- `groupingBy(f, TreeMap::new, toList())` → chaves ordenadas (determinístico).
- A opção B `{a=2, ...}` corresponde a `groupingBy(f)` + `counting()` como
  downstream — outro clássico de confusão.