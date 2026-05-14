package com.empik.coupons.infrastructure.geoip;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Konfiguracja klienta geo-IP
 */
@ConfigurationProperties(prefix = "app.geoip")
record GeoIpProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
