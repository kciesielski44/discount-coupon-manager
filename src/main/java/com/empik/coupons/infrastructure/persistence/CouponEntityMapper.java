package com.empik.coupons.infrastructure.persistence;

import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCode;

/**
 * Ręczny mapper między agregatem domenowym a encją JPA
 * Świadoma rezygnacja z MapStruct - przy jednym agregacie generator jest
 * dodatkową zależnością i nie wnosi sensownej wartości (więcej konfiguracji niż mapowania)
 * Ręczne mapowanie w tym przypadku pozostaje czytelne i debugowalne.
 */
final class CouponEntityMapper {

    private CouponEntityMapper() {}

    static CouponEntity toEntity(Coupon coupon) {
        return new CouponEntity(
                coupon.id(),
                coupon.code().value(),
                coupon.createdAt(),
                coupon.maxUsages(),
                coupon.currentUsages(),
                coupon.country().value()
        );
    }

    static Coupon toDomain(CouponEntity entity) {
        return new Coupon(
                entity.getId(),
                new CouponCode(entity.getCode()),
                entity.getCreatedAt(),
                entity.getMaxUsages(),
                entity.getCurrentUsages(),
                new CountryCode(entity.getCountry())
        );
    }
}
