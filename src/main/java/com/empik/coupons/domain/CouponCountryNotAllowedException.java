package com.empik.coupons.domain;

/**
 * Próba użycia kuponu z kraju innego niż wymagany przez kupon
 */
public final class CouponCountryNotAllowedException extends CouponDomainException {

    private final CountryCode clientCountry;
    private final CountryCode requiredCountry;

    public CouponCountryNotAllowedException(CouponCode code, CountryCode clientCountry, CountryCode requiredCountry) {
        super(
                "Coupon '%s' is restricted to country %s, client country: %s"
                        .formatted(code.value(), requiredCountry.value(), clientCountry.value()),
                code
        );
        this.clientCountry = clientCountry;
        this.requiredCountry = requiredCountry;
    }

    public CountryCode clientCountry() {
        return clientCountry;
    }

    public CountryCode requiredCountry() {
        return requiredCountry;
    }

    @Override
    public CouponErrorCode errorCode() {
        return CouponErrorCode.COUNTRY_NOT_ALLOWED;
    }
}
