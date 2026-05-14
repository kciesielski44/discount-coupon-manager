package com.empik.coupons.domain;

/**
 * System nie umie ustalić kraju klienta dla podanego IP
 */
public final class ClientCountryUnresolvedException extends CouponDomainException {

    private final String clientIp;

    public ClientCountryUnresolvedException(CouponCode code, String clientIp) {
        super(
                "Cannot resolve client country for ip '%s' (coupon '%s')"
                        .formatted(clientIp, code.value()),
                code
        );
        this.clientIp = clientIp;
    }

    public String clientIp() {
        return clientIp;
    }

    @Override
    public CouponErrorCode errorCode() {
        return CouponErrorCode.CLIENT_COUNTRY_UNRESOLVED;
    }
}
