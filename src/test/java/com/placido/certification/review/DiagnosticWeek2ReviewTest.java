package com.placido.certification.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

/**
 * Validacao com JVM real das 3 questoes erradas da Semana 2 do diagnostico
 * (1Z0-830 diagnostic, sessao 2026-09-10-diagnostic-01).
 *
 * Q05  -> StringBuilder insert + replace (language-basics, gabarito B)
 * Q07  -> LocalDate.plusYears em ano bissexto (date-time, gabarito A)
 * Q09  -> ZonedDateTime.withZoneSameInstant com DST (date-time, gabarito D)
 */
class DiagnosticWeek2ReviewTest {

    // === Q05: StringBuilder.insert + replace com indice half-open ===

    @Test
    void q05_stringBuilderInsertShiftsThenReplaceIsHalfOpen() {
        StringBuilder sb = new StringBuilder("Hello");

        sb.insert(3, "lo");          // insere antes do indice 3: "Hel" + "lo" + "lo" = "Hellolo"
        assertEquals("Hellolo", sb.toString());

        sb.replace(1, 3, "a");       // substitui [1,3): "e" e "l" viram "a"
        assertEquals("Halolo", sb.toString());
    }

    // === Q07: LocalDate.plusYears ajusta para o ultimo dia valido do mes ===

    @Test
    void q07_plusYearsAfterLeapDayRollsBackToFeb28() {
        LocalDate date = LocalDate.of(2024, 2, 29);

        assertTrue(date.isLeapYear());
        assertEquals(LocalDate.of(2025, 2, 28), date.plusYears(1));
        // 2028 volta a ser bissexto: o dia 29 e preservado
        assertEquals(LocalDate.of(2028, 2, 29), date.plusYears(4));
    }

    // === Q09: withZoneSameInstant preserva o instante (DST: EDT = UTC-4) ===

    @Test
    void q09_withZoneSameInstantFromEdtToJst() {
        ZonedDateTime zdt = ZonedDateTime.of(
                2024, 7, 15, 10, 30, 0, 0, ZoneId.of("America/New_York"));
        ZonedDateTime converted = zdt.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));

        // Julho em NY e EDT (UTC-4); a pegadinha e nao assumir EST (-5)
        assertEquals("-04:00", zdt.getOffset().getId());
        assertEquals("+09:00", converted.getOffset().getId());

        // withZoneSameInstant preserva o INSTANTE: os dois temporais sao o
        // mesmo ponto no tempo, logo Duration entre eles e ZERO (nao 13h).
        // As 13h sao a diferenca entre os OFFSETS das zonas, nao entre instantes.
        assertEquals(zdt.toInstant(), converted.toInstant());
        assertEquals(Duration.ZERO, Duration.between(zdt, converted));
        long offsetDiffSeconds = converted.getOffset().getTotalSeconds()
                - zdt.getOffset().getTotalSeconds();
        assertEquals(Duration.ofHours(13), Duration.ofSeconds(offsetDiffSeconds));

        assertEquals(23, converted.getHour());
        assertEquals(15, converted.getDayOfMonth());
    }
}