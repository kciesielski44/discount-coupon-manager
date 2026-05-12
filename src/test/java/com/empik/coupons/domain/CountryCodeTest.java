package com.empik.coupons.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryCodeTest {

    @Test
    void acceptsValidIsoCode() {
        assertThat(new CountryCode("PL").value())
                .isEqualTo("PL");
    }

    @Test
    void normalizesToUpperCase() {
        assertThat(new CountryCode("pl").value())
                .isEqualTo("PL");
    }

    @Test
    void mixedCaseProducesEqualValueObjects() {
        assertThat(new CountryCode("De"))
                .isEqualTo(new CountryCode("DE"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "P", "POL", "XX", "12", "P1"})
    void rejectsInvalidValues(String input) {
        assertThatThrownBy(() -> new CountryCode(input))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
