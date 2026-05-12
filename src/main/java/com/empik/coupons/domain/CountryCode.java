package com.empik.coupons.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Kraj w formacie ISO 3166-1 alpha-2 (PL, DE, US, itd). Wartości upper case
 */
public record CountryCode(String value) {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public CountryCode {
        if (value == null) {
            throw new IllegalArgumentException("Country code must not be null");
        }
        value = value.toUpperCase(Locale.ROOT);
        if (!ISO_COUNTRIES.contains(value)) {
            throw new IllegalArgumentException(
                    "Country code must be ISO 3166-1 alpha-2. Value: " + value);
        }
    }
}
