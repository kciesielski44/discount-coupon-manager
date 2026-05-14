package com.empik.coupons.infrastructure.geoip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO odpowiedzi ip-api
 */
@JsonIgnoreProperties(ignoreUnknown = true) //ignorowanie nieznanych pól
record IpApiResponse(String status, String countryCode) {

    boolean successful() {
        return "success".equalsIgnoreCase(status);
    }
}
