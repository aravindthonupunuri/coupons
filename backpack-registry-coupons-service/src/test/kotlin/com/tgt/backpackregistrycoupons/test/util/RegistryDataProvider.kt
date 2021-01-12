package com.tgt.backpackregistrycoupons.test.util

import com.tgt.backpackregistryclient.transport.*
import com.tgt.backpackregistryclient.util.*
import com.tgt.backpackregistrycoupons.migration.model.RegistryCouponMetaDataTO
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.cronbeacon.kafka.model.CronEvent
import com.tgt.lists.atlas.api.type.UserMetaData.Companion.toUserMetaData
import java.time.*
import java.util.*

@Suppress("UNCHECKED_CAST")
class RegistryDataProvider {

    fun getRegistryMetaDataMap(
        profileAddressId: UUID?,
        giftCardsEnabled: Boolean?,
        groupGiftEnabled: Boolean?,
        groupGiftAmount: String?,
        recipients: List<RegistryRecipientTO>?,
        event: RegistryEventTO?,
        babyRegistry: RegistryBabyTO?,
        guestRulesMetaData: RegistryGuestRuleMetaDataTO?,
        imageMetaData: RegistryImageMetaDataTO?,
        customUrl: String?
    ): Map<String, Any>? {
        return toUserMetaData(RegistryMetaDataTO.toStringRegistryMetadata(RegistryMetaDataTO(profileAddressId, giftCardsEnabled, groupGiftEnabled,
            groupGiftAmount, recipients, event, babyRegistry, guestRulesMetaData, imageMetaData, customUrl)))?.metadata
    }

    fun getRegistryCouponMetaDataMap(
        onlineCouponCode: String,
        onlineCouponStatus: CouponRedemptionStatus,
        storeCouponCode: String,
        storeCouponStatus: CouponRedemptionStatus,
        couponIssueDate: LocalDate,
        couponExpiryDate: LocalDate
    ): Map<String, Any>? {
        return toUserMetaData(RegistryCouponMetaDataTO.toStringRegistryCouponMetadata(RegistryCouponMetaDataTO(onlineCouponCode, onlineCouponStatus, storeCouponCode,
                    storeCouponStatus, couponIssueDate, couponExpiryDate)))?.metadata
    }

    fun createCronEvent(eventLocalDateTime: LocalDateTime, minuteBlockOfHour: Long, eventIntervalMinutes: Long, timeZoneId: ZoneId): CronEvent {
        return CronEvent(eventDateTime = eventLocalDateTime,
            timeZone = timeZoneId.id,
            eventIntervalMins = eventIntervalMinutes,
            minuteBlockOfHour = minuteBlockOfHour,
            hourOfDay = eventLocalDateTime.hour,
            dayOfWeek = eventLocalDateTime.dayOfWeek,
            dayOfMonth = eventLocalDateTime.dayOfMonth,
            monthOfYear = eventLocalDateTime.month
        )
    }

    fun getRegistryDetails(registryId: String): RegistryDetailsResponseTO {
        return RegistryDetailsResponseTO(UUID.fromString(registryId), "", "", null, null, "regfname", "reglname", "coregfname", "coreglname", LocalDate.now())
    }
}
