package com.grupo.formslp.omnipages.page.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LandingPageSpecificationsTest {

    @Test
    void escapeLikeTreatsWildcardsAsText() {
        assertEquals("50\\% off", LandingPageSpecifications.escapeLike("50% off"));
        assertEquals("pagina\\_1", LandingPageSpecifications.escapeLike("pagina_1"));
        assertEquals("a\\\\b", LandingPageSpecifications.escapeLike("a\\b"));
        assertEquals("black friday", LandingPageSpecifications.escapeLike("black friday"));
    }
}
