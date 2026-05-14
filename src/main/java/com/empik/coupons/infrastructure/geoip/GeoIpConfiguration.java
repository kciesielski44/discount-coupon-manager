package com.empik.coupons.infrastructure.geoip;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Konfiguracja klienta HTTP dla geo-IP
 */
@Configuration
@EnableCaching(proxyTargetClass = true)
@EnableConfigurationProperties(GeoIpProperties.class)
class GeoIpConfiguration {

    @Bean
    RestClient geoIpRestClient(GeoIpProperties properties) {
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }
}
