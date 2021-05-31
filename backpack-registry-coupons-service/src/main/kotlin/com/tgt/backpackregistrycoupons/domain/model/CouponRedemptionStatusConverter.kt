package com.tgt.backpackregistrycoupons.domain.model

import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpackregistrycoupons.util.toCouponRedemptionStatus
import io.micronaut.context.annotation.Factory
import io.micronaut.core.convert.TypeConverter
import java.util.*
import javax.inject.Singleton

@Factory
class CouponRedemptionStatusConverter {
    @Singleton
    fun couponRedemptionStatusToStringTypeConverter(): TypeConverter<CouponRedemptionStatus, String> {
        return TypeConverter { status, targetType, context ->
            Optional.of<String>(status.name)
        }
    }

    @Singleton
    fun stringToCouponRedemptionStatusConverter(): TypeConverter<String, CouponRedemptionStatus> {
        return TypeConverter { str, targetType, context ->
            Optional.of<CouponRedemptionStatus>(toCouponRedemptionStatus(str)) }
    }
}
