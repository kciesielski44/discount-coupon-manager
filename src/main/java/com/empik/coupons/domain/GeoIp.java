package com.empik.coupons.domain;

import java.util.Optional;

/**
 * Port wyjściowy: ustalanie kraju klienta po adresie IP
 *
 * Empty Optional = brak wiarygodnej odpowiedzi: dostawca nie zna lokalizacji, sieć padła, itp.
 * Warstwa aplikacji nie rozróżnia tych powodów
 */
public interface GeoIp {

    Optional<CountryCode> lookup(String ipAddress);
}
