package com.tgt.backpackregistrycoupons.test.util

import com.tgt.backpackregistryclient.transport.*
import com.tgt.backpackregistryclient.util.RegistrySearchVisibility
import com.tgt.backpackregistryclient.util.RegistryStatus
import com.tgt.backpackregistryclient.util.RegistryType
import com.tgt.cronbeacon.kafka.model.CronEvent
import com.tgt.lists.atlas.api.type.UserMetaData.Companion.toUserMetaData
import java.time.*
import java.util.*

@Suppress("UNCHECKED_CAST")
class RegistryDataProvider {

    fun getRegistryMetaDataMap(
        profileAddressId: UUID?,
        alternateRegistryId: String?,
        giftCardsEnabled: Boolean?,
        groupGiftEnabled: Boolean?,
        groupGiftAmount: String?,
        recipients: List<RegistryRecipientTO>?,
        event: RegistryEventTO?,
        babyRegistry: RegistryBabyTO?,
        guestRulesMetaData: RegistryGuestRuleMetaDataTO?,
        imageMetaData: RegistryImageMetaDataTO?,
        customUrl: String?,
        organizationName: String?,
        occasionName: String?
    ): Map<String, Any>? {
        return toUserMetaData(RegistryMetaDataTO.toStringRegistryMetadata(RegistryMetaDataTO(profileAddressId, alternateRegistryId,
            giftCardsEnabled, groupGiftEnabled, groupGiftAmount, recipients, event, babyRegistry, guestRulesMetaData, imageMetaData,
            customUrl, organizationName, occasionName)))?.metadata
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
        return RegistryDetailsResponseTO(UUID.fromString(registryId), "", "", null, null, null, null, null, RegistrySearchVisibility.PUBLIC, RegistryType.BABY,
            RegistryStatus.ACTIVE, null, null)
    }
}
