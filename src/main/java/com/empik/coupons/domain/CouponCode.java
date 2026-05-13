package com.empik.coupons.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Kod kuponu (wartość "value" jest case-insensitive, przechowywana jako znormalizowany lower case)
 */
public record CouponCode(String value) {

    /**
     * Litery ASCII (małe i duże), cyfry, podkreślnik i myślnik; długość od 1 do 64 znaków
     * Bez białych znaków i znaków spoza ASCII (kod ma być bezpieczny w URL i logach)
     */
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    public CouponCode {
        if (value == null) {
            throw new IllegalArgumentException("Coupon code must not be null");
        }
        if (!ALLOWED.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Coupon code must match [A-Za-z0-9_-]{1,64}. Value: " + value);
        }
        value = value.toLowerCase(Locale.ROOT);
    }
}
