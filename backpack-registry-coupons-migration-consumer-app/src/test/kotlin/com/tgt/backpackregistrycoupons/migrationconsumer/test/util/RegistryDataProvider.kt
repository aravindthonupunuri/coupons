package com.tgt.backpackregistrycoupons.migrationconsumer.test.util

import com.tgt.backpackregistrycoupons.kafka.migration.model.RegistryCouponMetaDataTO
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.lists.atlas.api.type.UserMetaData
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("UNCHECKED_CAST")
class RegistryDataProvider {

    fun getRegistryCouponMetaDataMap(
        onlineCouponCode: String,
        onlineCouponStatus: CouponRedemptionStatus,
        storeCouponCode: String,
        storeCouponStatus: CouponRedemptionStatus,
        couponIssueDate: LocalDate,
        couponExpiryDate: LocalDate,
        addedDate: LocalDateTime,
        lastModifiedTime: LocalDateTime
    ): Map<String, Any>? {
        return UserMetaData.toUserMetaData(RegistryCouponMetaDataTO.toStringRegistryCouponMetadata(RegistryCouponMetaDataTO(onlineCouponCode, onlineCouponStatus, storeCouponCode,
            storeCouponStatus, couponIssueDate, couponExpiryDate, addedDate, lastModifiedTime)))?.metadata
    }
}
