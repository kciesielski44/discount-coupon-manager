package com.empik.coupons.infrastructure.persistence;

import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CouponEntityMapperTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Instant CREATED_AT = Instant.parse("2026-05-13T10:15:30Z");

    @Test
    void mapsDomainToEntityUsingNormalizedValues() {
        Coupon coupon = new Coupon(ID, new CouponCode("Wiosna"), CREATED_AT, 10, 3, new CountryCode("pl"));

        CouponEntity entity = CouponEntityMapper.toEntity(coupon);

        assertThat(entity.getId()).isEqualTo(ID);
        assertThat(entity.getCode()).isEqualTo("wiosna");
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getMaxUsages()).isEqualTo(10);
        assertThat(entity.getCurrentUsages()).isEqualTo(3);
        assertThat(entity.getCountry()).isEqualTo("PL");
    }

    @Test
    void roundtripPreservesDomainState() {
        Coupon original = new Coupon(ID, new CouponCode("promo_2026"), CREATED_AT, 5, 1, new CountryCode("DE"));

        Coupon roundtripped = CouponEntityMapper.toDomain(CouponEntityMapper.toEntity(original));

        assertThat(roundtripped).isEqualTo(original);
    }
}
