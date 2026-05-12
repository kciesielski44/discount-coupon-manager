package com.empik.coupons.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponCodeTest {

    @Test
    void normalizeToLowerCase() {
        assertThat(new CouponCode("WIOSNA").value())
                .isEqualTo("wiosna");
    }

    @Test
    void mixedCaseProducesEqualValueObjects() {
        assertThat(new CouponCode("WIOSNA"))
                .isEqualTo(new CouponCode("Wiosna"));
    }

    @Test
    void acceptsAllowedCharacters() {
        assertThat(new CouponCode("PROMO_2026-Q1").value())
                .isEqualTo("promo_2026-q1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "with space", "ąż", "promo!", "promo#1"})
    void rejectsInvalidValues(String input) {
        assertThatThrownBy(() -> new CouponCode(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCodeLongerThanLimit() {
        assertThatThrownBy(() -> new CouponCode("a".repeat(65)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsMaximumLength() {
        String maxLen = "a".repeat(64);
        assertThat(new CouponCode(maxLen).value())
                .isEqualTo(maxLen);
    }
}
