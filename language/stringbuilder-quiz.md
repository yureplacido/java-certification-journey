# Quiz — StringBuilder: insert / replace / delete / append com índices (estilo 1Z0-830)

**Tópico:** language (1Z0-830 Seção 1 — Language Basics) · **Java 21**
**Formato:** 10 questões de múltipla escolha, 4 alternativas (A–D), uma única correta.
**Validação:** todas as saídas e exceções conferidas por execução real em JVM 21.0.2
(`/tmp/opencode/quiz-probe.java`).

## Instruções

- Resolva **de cabeça** antes de olhar a resposta: reescreva o estado do
  `StringBuilder` char a char (com índices 0-based) **após cada operação**.
- Regras que regem o quiz (não vale consultar durante a prova):
  - `insert(offset, s)` insere **antes** do char em `offset`; o resto desloca à direita.
  - `replace(start, end, s)` e `delete(start, end)` usam intervalo **half-open**
    `[start, end)` — end exclusivo.
  - `end` acima de `length()` é **truncado** (não lança); `start < 0`,
    `start > length()` ou `start > end` lança `StringIndexOutOfBoundsException`.
  - `insert(pos, (String) null)` insere o literal `"null"` (não lança NPE).
  - `String.replace(CharSequence, CharSequence)` substitui **texto literal em todas
    as ocorrências**; `StringBuilder.replace(start, end, s)` é por **índices de char**.
- Resposta e explicação aparecem logo abaixo de cada questão (material de estudo
  individual) — o gabarito consolidado fica na seção final.

---

### Questão 1

O que é impresso?

```java
StringBuilder sb = new StringBuilder("abcde");
sb.replace(1, 3, "123");
sb.replace(2, 4, "X");
System.out.println(sb);
```

A) `a1Xde`
B) `a123de`
C) `a1X23de`
D) `a1Xe`

**Resposta:** A

**Explicação:** Após `replace(1, 3, "123")` o estado é `a123de`
(`a` + `123` + `de`; o intervalo `[1,3)` removeu `bc`). O segundo `replace(2, 4, "X")`
opera sobre esse **estado atual**: `[2,4)` remove `2` e `3`, e insere `X` na posição 2
→ `a1Xde`. O `end` é exclusivo: o char no índice 4 (`d`) não foi tocado.

**Trap:** #1 — `end` exclusivo no `replace`; #3 — recalcular índices sobre o estado
após a operação anterior.

---

### Questão 2

O que é impresso?

```java
StringBuilder sb = new StringBuilder("WXYZ");
sb.append("!");
sb.insert(2, "##");
sb.insert(0, sb.charAt(3));
System.out.println(sb);
```

A) `#WX##YZ!`
B) `WX##YZ!`
C) `WX##YZ!#`
D) `ZWX##YZ!`

**Resposta:** A

**Explicação:** `append("!")` → `WXYZ!`. `insert(2, "##")` insere **antes** do char do
índice 2 (`Y`) e desloca o resto → `WX##YZ!`. Na última linha, os argumentos são
avaliados **antes** da mutação: `sb.charAt(3)` lê o estado atual (`WX##YZ!`, índice 3
= `#`) e então `insert(0, '#')` insere no início → `#WX##YZ!`.

**Trap:** #2 — `insert` insere antes do offset e desloca o resto à direita; #3 — o
argumento `charAt(3)` é lido do estado após o `insert(2, "##")`, não do estado original.

---

### Questão 3

O que é impresso?

```java
StringBuilder sb = new StringBuilder("12345678");
sb.delete(2, 5);
sb.insert(2, "X");
sb.replace(2, 3, "Y");
System.out.println(sb);
```

A) `12Y678`
B) `12Y78`
C) `12X678`
D) `1YX678`

**Resposta:** A

**Explicação:** `delete(2, 5)` (half-open) remove `3`, `4`, `5` → `12678`.
`insert(2, "X")` → `12X678`. `replace(2, 3, "Y")` troca exatamente o `X`
(`[2,3)` é 1 char) → `12Y678`. Depois de um `delete` que encurta a string,
os índices de tudo à esquerda do ponto de remoção permanecem, mas o conteúdo à
direita **muda de posição** — o `6`, `7`, `8` agora ocupam os índices 3, 4, 5, e é
sobre eles que as próximas operações indexam.

**Trap:** #10 — operações que encurtam/esticam o StringBuilder reancoram o significado
do próximo índice (recálculo obrigatório); #3 — estado atual a cada passo.

---

### Questão 4

O que é impresso?

```java
StringBuilder sb = new StringBuilder("12");
sb.insert(0, (String) null);
sb.append(sb.charAt(0));
sb.insert(3, '!');
System.out.println(sb);
```

A) `nul!l12n`
B) `null!12n`
C) Lança `NullPointerException` na linha `sb.insert(0, (String) null)`
D) `null12n`

**Resposta:** A

**Explicação:** `sb.insert(0, (String) null)` **não lança NPE** — a API converte `null`
no literal `"null"` → `null12`. `append(sb.charAt(0))` avalia `charAt(0)` = `n` e
anexa → `null12n`. `insert(3, '!')` insere antes do índice 3 (o segundo `l`)
→ `nul` + `!` + `l12n` = `nul!l12n`.

**Trap:** #5 — `insert(pos, (String) null)` insere o texto literal `"null"`; o erro
clássico é responder NPE (alternativa C). A alternativa B inverte a semântica do
`insert` (inserir *depois* do índice 3).

---

### Questão 5

O que é impresso?

```java
StringBuilder sb = new StringBuilder("abcde");
sb.replace(3, 99, "X");
sb.append("!");
System.out.println(sb);
```

A) `abcX!`
B) `abcX`
C) `abcXde!`
D) Lança `StringIndexOutOfBoundsException` na linha `sb.replace(3, 99, "X")`

**Resposta:** A

**Explicação:** `end` acima de `length()` **não lança exceção**: é truncado para
`length()` (5). `replace(3, 5, "X")` remove `de` e insere `X` → `abcX`. O `append`
adiciona `!` → `abcX!`. A alternativa C vem de truncar `end` até `start`
(equivalendo a um insert), o que a API não faz.

**Trap:** #4 — `end` acima de `length()` é truncado, nunca vira exceção; apenas
`start` inválido (`< 0`, `> length()`, ou `start > end`) lança
`StringIndexOutOfBoundsException`.

---

### Questão 6

O que é impresso?

```java
StringBuilder sb = new StringBuilder("banana");
sb.append(sb.toString().replace("a", "X"));
sb.replace(2, 4, "NA");
System.out.println(sb);
```

A) `baNAnabXnXnX`
B) `baNANAbXnXnX`
C) `baNAnabXnana`
D) `bananabXnXnX`

**Resposta:** A

**Explicação:** `sb.toString().replace("a", "X")` é `String.replace(CharSequence,
CharSequence)` — substitui **todas as ocorrências** do literal `"a"` → `bXnXnX`;
`append` → `bananabXnXnX`. O `sb.replace(2, 4, "NA")` é por **índices de char**:
`[2,4)` = `na` → remove e insere `NA` na posição 2 → `baNAnabXnXnX`. As duas APIs
têm semânticas diferentes e aparecem lado a lado de propósito.

**Trap:** #6 — confusão intencional `String.replace` (literal, todas as ocorrências —
usada corretamente na primeira linha) vs `StringBuilder.replace(start, end, str)`
(índices de char — usada na segunda). A alternativa B aplica semântica de texto
literal ao `StringBuilder.replace`; a C troca `String.replace` por "só a primeira
ocorrência".

---

### Questão 7

O que é impresso?

```java
StringBuilder sb = new StringBuilder("AC");
sb.replace(1, 1, "B");
sb.replace(0, 0, sb.charAt(2) + "");
System.out.println(sb);
```

A) `CABC`
B) `ABC`
C) `CBC`
D) Lança `StringIndexOutOfBoundsException` na linha `sb.replace(0, 0, sb.charAt(2) + "")`

**Resposta:** A

**Explicação:** `replace(1, 1, "B")` com `start == end` insere no meio (equivale a
`insert(1, "B")`) → `ABC`. Na linha seguinte os argumentos são avaliados antes da
invocação: `sb.charAt(2)` lê o estado atual `ABC` → `'C'`; `replace(0, 0, "C")` insere
no início → `CABC`. Quem ignora o primeiro replace considera o estado `AC`
(length 2) e o `charAt(2)` lançaria `StringIndexOutOfBoundsException` — exatamente o
que a alternativa D (incorreta) afirma.

**Trap:** #7 — `start == end` num replace é inserção pura (não é no-op); #3 — o
argumento `charAt(2)` é avaliado sobre o estado **após** o primeiro `replace`.

---

### Questão 8

O que acontece ao executar?

```java
StringBuilder sb = new StringBuilder("JAVA21");
sb.delete(0, 4);
sb.delete(1, 5);
sb.append("J");
sb.delete(1, 0);
System.out.println(sb);
```

A) Imprime `2J`
B) Imprime `2J` e depois a exceção é lançada
C) Lança `StringIndexOutOfBoundsException` na linha `sb.delete(1, 0)`; nada é impresso
D) Lança `StringIndexOutOfBoundsException` na linha `sb.delete(1, 5)`

**Resposta:** C

**Explicação:** `delete(0, 4)` (half-open) remove `JAVA` → `21`. `delete(1, 5)` tem
`end > length()`: o end é truncado para 2, removendo `[1,2)` = `1` → `2`.
`append("J")` → `2J` (length 2). `delete(1, 0)` tem `start > end` → lança
`StringIndexOutOfBoundsException` **antes** do `System.out.println` — a cadeia é
interrompida e nada é impresso. A alternativa D inverte a regra: `end` além de
`length()` trunca, não lança.

**Trap:** #8 — `delete` também é half-open; #9 — a exceção interrompe a cadeia e o
`println` nunca executa (identificar a linha que lança); #4 — `end > length()`
trunca em `delete` também.

---

### Questão 9

O que acontece ao executar?

```java
StringBuilder sb = new StringBuilder("abcdef");
sb.delete(0, 2);
sb.insert(2, "XY");
sb.replace(4, 6, "!!");
sb.delete(2, 8);
sb.insert(5, "?");
System.out.println(sb);
```

A) Imprime `cd`
B) Imprime `cdXY`
C) Lança `StringIndexOutOfBoundsException` na linha `sb.insert(5, "?")`; nada é impresso
D) Lança `StringIndexOutOfBoundsException` na linha `sb.delete(2, 8)`

**Resposta:** C

**Explicação:** Rastreando o estado: `delete(0, 2)` → `cdef`; `insert(2, "XY")` →
`cdXYef`; `replace(4, 6, "!!")` → `cdXY!!`; `delete(2, 8)` com `end` truncado para 6
remove `[2,6)` = `XY!!` → `cd` (length 2). O `insert(5, "?")` tem `offset` **acima de
`length()`** (5 > 2) → lança `StringIndexOutOfBoundsException` nessa linha, e o
`println` não roda. A linha `delete(2, 8)` é válida (truncamento de end); quem erra
qualquer um dos quatro passos anteriores aponta a linha errada.

**Trap:** #9 — só o estado final da cadeia importa e a exceção interrompe no ponto
exato (é preciso recalcular a cadeia inteira para saber que o length é 2); #3 —
índices sempre sobre o estado atual.

---

### Questão 10

O que é impresso?

```java
StringBuilder sb = new StringBuilder("ab");
sb.append("12");
sb.insert(1, "XY");
sb.replace(2, 5, "z");
sb.delete(0, 1);
sb.insert(sb.length(), "!");
System.out.println(sb);
```

A) `Xz2!`
B) `aXz2`
C) `Xz!`
D) `Xz12!`

**Resposta:** A

**Explicação:** `append("12")` → `ab12`; `insert(1, "XY")` → `aXYb12`;
`replace(2, 5, "z")` remove `[2,5)` = `Yb1` → `aXz2` (o `2` deslocou-se do índice 5
para o 3 — recálculo); `delete(0, 1)` remove o `a` → `Xz2`; `insert(sb.length(), "!")`
com `offset == length()` equivale a `append` → `Xz2!`. Cada operação estica ou
encurta a string e redefine o significado dos índices seguintes: quem usa os índices
da etapa anterior erra o `replace` e/ou o `delete`.

**Trap:** #10 — operações que encurtam/esticam mudam o significado do próximo índice;
#3 — recalcular o estado completo char a char a cada operação.

---

## Gabarito

| Questão | Resposta | Saída / Comportamento verificado | Trap exercitada |
| --- | --- | --- | --- |
| 1 | A | `a1Xde` | #1 end exclusivo + #3 recalcular estado |
| 2 | A | `#WX##YZ!` | #2 insert antes do offset (desloca à direita) + #3 argumento lido do estado atual |
| 3 | A | `12Y678` | #10 encurtamento reancora índices + #3 |
| 4 | A | `nul!l12n` | #5 `insert(pos, (String) null)` insere o literal `"null"` |
| 5 | A | `abcX!` | #4 `end > length()` trunca — não lança |
| 6 | A | `baNAnabXnXnX` | #6 `String.replace` (literal, todas) vs `StringBuilder.replace` (índices) |
| 7 | A | `CABC` | #7 `start == end` insere (equivale a insert) |
| 8 | C | nada impresso — `StringIndexOutOfBoundsException` em `delete(1, 0)` | #8 delete half-open + #9 exceção interrompe + #4 truncamento |
| 9 | C | nada impresso — `StringIndexOutOfBoundsException` em `insert(5, "?")` | #9 identificar a linha que lança + #3 cadeia completa |
| 10 | A | `Xz2!` | #10 esticar/encurtar reancora índices + #3 |

Saídas e exceções confirmadas por execução real (JVM 21.0.2):
`a1Xde`, `#WX##YZ!`, `12Y678`, `nul!l12n`, `abcX!`, `baNAnabXnXnX`, `CABC`, `Xz2!`,
e as exceções `StringIndexOutOfBoundsException: Range [1, 0) out of bounds for length 2`
(Q8, linha `delete(1, 0)`) e `StringIndexOutOfBoundsException: Range [5, 2) out of
bounds for length 2` (Q9, linha `insert(5, "?")`).