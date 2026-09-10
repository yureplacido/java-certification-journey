package com.placido.certification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmokeTest {

    @Test
    void baselineProjectIsReady() {
        assertEquals("java-certification-journey", App.projectName());
    }
}