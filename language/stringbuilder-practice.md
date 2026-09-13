# Prática — StringBuilder: insert/replace com índices

**Objetivo:** automatizar o recálculo de índices após operações mutáveis e
consolidar o intervalo half-open. Cada exercício foi *resolvido de cabeça*
primeiro e *validado por execução* (JVM 21.0.2). O gabarito fica abaixo.

**Regras fixadas:**

- `insert(offset, s)`: insere antes do char em `offset`; válido de `0` a `length()`.
- `replace(start, end, s)`: substitui `[start, end)` — end **exclusivo**.
- `end` acima de `length()` é truncado (não lança); `start` fora de `[0, length]`
  ou `start > end` lança `StringIndexOutOfBoundsException`.
- `insert(pos, (String) null)` insere o literal `"null"`.

---

## Exercícios

### Ex1 — O que é impresso?

```java
StringBuilder sb = new StringBuilder("ab");
sb.insert(1, "X");        // antes do char 1 -> "aXb"
sb.replace(0, 2, "Z");    // [0,2): "aX" -> "Z"
System.out.println(sb);
```

**Minha resposta de cabeça:** `Zb`

### Ex2 — O que é impresso?

```java
StringBuilder sb = new StringBuilder("01234");
sb.delete(1, 3);                // remove [1,3): "12" -> "034"
sb.insert(sb.length(), "5");    // append no final
sb.replace(1, 2, "-");          // [1,2): "3" -> "-"
System.out.println(sb);
```

**Minha resposta de cabeça:** `0-45`

### Ex3 — O que é impresso?

```java
StringBuilder sb = new StringBuilder("Aa");
sb.replace(0, 0, "X");    // insere "X" no inicio (start == end)
sb.replace(2, 2, "Y");    // insere "Y" depois de "Xa"
System.out.println(sb);
```

**Minha resposta de cabeça:** `XaYa` ✗

### Ex4 — O que é impresso?

```java
StringBuilder sb = new StringBuilder("Hello");
sb.replace(3, 99, "");    // end > length -> truncado em 5, remove [3,5)
System.out.println(sb);
```

**Minha resposta de cabeça:** `Hel`

### Ex5 — O que é impresso?

```java
StringBuilder sb = new StringBuilder("null");
sb.insert(0, (String) null);  // insere o literal "null" (nao lanca NPE)
System.out.println(sb);
```

**Minha resposta de cabeça:** `nullnull`

### Ex6 — O que é impresso?

```java
StringBuilder sb = new StringBuilder("What's up?");
sb.insert(5, " going");
sb.replace(0, 5, "How's");
System.out.println(sb);
```

**Minha resposta de cabeça:** `How's going up?` ✗

---

## Gabarito (verificado por execução)

| Ex | Saída real | Minha resposta | Correto? |
| --- | --- | --- | --- |
| Ex1 | `Zb` | `Zb` | sim |
| Ex2 | `0-45` | `0-45` | sim |
| Ex3 | `XAYa` | `XaYa` | **não** — caso do `A` |
| Ex4 | `Hel` | `Hel` | sim |
| Ex5 | `nullnull` | `nullnull` | sim |
| Ex6 | `How's goings up?` | `How's going up?` | **não** — esqueci o `" s"` final |

## Análise dos erros

**Ex3 (`XAYa` vs `XaYa`):** a string original é `"Aa"` (`A` minúsculo em posição
diferente). `replace(0, 0, "X")` insere antes de 0 → `"XAa"`; `replace(2, 2, "Y")`
insere antes do índice 2 (`a` minúsculo) → `"XAYa"`. Errei por escrever a string no
papel com caixa errada.

**Ex6 (`How's goings up?` vs `How's going up?`):** após `insert(5, " going")`, o
conteúdo é `"What' goings up?"` — as `going` entram antes do `s`, e o `s` do `What's`
permanece. `replace(0, 5, "How's")` troca `"What'"` → o `" goings up?"` fica intacto.
Lição: **reescrever a string inteira no papel após cada operação**, conservando
caracteres que não foram tocados.