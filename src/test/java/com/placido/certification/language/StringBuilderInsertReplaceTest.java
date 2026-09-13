package com.placido.certification.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Experimento minimo (1Z0-830 Language Basics): insert() desloca para a direita
 * e replace() usa intervalo half-open [start, end) — validacao com JVM real.
 */
class StringBuilderInsertReplaceTest {

    // Experimento 1: insert em meio de string desloca o restante para a direita.
    @Test
    void insertMidShiftsTailToTheRight() {
        StringBuilder sb = new StringBuilder("Hello");
        assertEquals("Hello", sb.toString());

        sb.insert(3, "lo"); // insere antes do indice 3
        assertEquals("Hellolo", sb.toString());

        sb.insert(0, ">>"); // caso de borda: indice 0 (insere no inicio)
        assertEquals(">>Hellolo", sb.toString());

        sb.insert(sb.length(), "<<"); // caso de borda: indice == length (append)
        assertEquals(">>Hellolo<<", sb.toString());
    }

    // Experimento 2: replace substitui [start, end) — end exclusivo sempre.
    @Test
    void replaceUsesHalfOpenRange() {
        StringBuilder sb = new StringBuilder("abcdef");

        sb.replace(1, 3, "XY"); // substitui indices 1 e 2 ("bc")
        assertEquals("aXYdef", sb.toString());

        sb.replace(0, 1, "Z"); // substitui apenas o indice 0 ("a")
        assertEquals("ZXYdef", sb.toString());

        sb.replace(5, 6, "!"); // end == length: substitui o ultimo char
        assertEquals("ZXYde!", sb.toString());
    }

    // Experimento 3: replace pode encurtar e estender o StringBuilder.
    @Test
    void replaceCanShrinkAndGrow() {
        StringBuilder sb = new StringBuilder("abc");

        sb.replace(0, 3, "x"); // intervalo inteiro vira "x" (encurta)
        assertEquals("x", sb.toString());

        sb.replace(0, 1, "verylong"); // extensao: [0,1) = "x" vira "verylong"
        assertEquals("verylong", sb.toString());
    }

    // Experimento 4: encadeamento de insert + delete com recalculculo de indices.
    @Test
    void chainedMutationsReevaluateIndicesOnCurrentState() {
        StringBuilder sb = new StringBuilder("Java");
        sb.insert(4, " SE");     // "Java SE"
        sb.delete(4, 7);         // remove [4,7): " SE"  -> "Java"
        sb.insert(0, "21 ");
        assertEquals("21 Java", sb.toString());
    }

    // Experimento 5: String.replace (literal, todas ocorrencias) vs
    // StringBuilder.replace (indices, uma ocorrencia).
    @Test
    void stringReplaceIsLiteralNotIndexBased() {
        String s = "banana";
        String literal = s.replace("an", "or"); // substitui TODAS as ocorrencias de "an"
        assertEquals("borora", literal);

        StringBuilder sb = new StringBuilder("banana");
        sb.replace(1, 3, "or"); // [1,3): 1a ocorrencia ("an" nos indices 1,2)
        assertEquals("borana", sb.toString());
    }

    // Experimento 6: validacao de indices.
    @Test
    void outOfBoundsIndicesThrow() {
        StringBuilder sb = new StringBuilder("abc");

        // insert: offset fora de [0, length] -> excecao
        assertThrows(StringIndexOutOfBoundsException.class, () -> sb.insert(4, "d"));
        assertThrows(StringIndexOutOfBoundsException.class, () -> sb.insert(-1, "d"));
        // replace: start > end -> excecao
        assertThrows(StringIndexOutOfBoundsException.class, () -> sb.replace(2, 1, "x"));
    }

    // Experimento 6b: VERIFICADO em JVM real — replace com end acima de length()
    // NAO lanca: o end e truncado ao tamanho da string.
    @Test
    void replaceEndBeyondLengthIsClamped() {
        StringBuilder sb = new StringBuilder("abc");
        sb.replace(2, 10, ""); // end truncado para 3 -> remove indices 2 ('c')
        assertEquals("ab", sb.toString());

        sb.replace(1, 99, "Z"); // end truncado para 2 -> [1,2) -> 'b' vira 'Z'
        assertEquals("aZ", sb.toString());
    }

    // Experimento 7: replace pode remover conteudo usando string vazia.
    @Test
    void deleteViaReplaceWithEmptyString() {
        StringBuilder sb = new StringBuilder("Hello World");
        sb.replace(5, 6, ""); // remove o espaco em [5,6)
        assertEquals("HelloWorld", sb.toString());
    }

    // Experimento 8: VERIFICADO em JVM real — insert(pos, (String) null) NAO
    // lanca NPE: insere o texto literal "null".
    @Test
    void insertNullInsertsLiteralNullText() {
        StringBuilder sb = new StringBuilder("Hello");
        sb.insert(1, (String) null);
        assertEquals("Hnullello", sb.toString());
    }

    // Experimento 9: replace com intervalo vazio (start == end) nao muda o conteudo.
    @Test
    void zeroLengthReplaceIsNoOp() {
        StringBuilder sb = new StringBuilder("abc");
        sb.replace(1, 1, "XYZ");
        assertEquals("aXYZbc", sb.toString());
    }

    // Experimento 10: insert de valor primitivo usa representacao do tipo.
    @Test
    void insertAppendsPrimitiveRepresentation() {
        StringBuilder sb = new StringBuilder("x");
        sb.insert(1, true);
        assertEquals("xtrue", sb.toString());

        StringBuilder sb2 = new StringBuilder().insert(0, 3.5);
        assertEquals("3.5", sb2.toString());
        assertTrue(sb2.toString().startsWith("3"));
    }
}