package com.tgt.backpackregistrycoupons.test.util

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.backpackregistryclient.transport.*
import com.tgt.backpackregistryclient.transport.RegistryItemMetaDataTO.Companion.toStringRegistryItemMetaData
import com.tgt.backpackregistryclient.util.*
import com.tgt.backpackregistrycoupons.migration.model.RegistryCouponMetaDataTO
import com.tgt.backpackregistrycoupons.util.CouponRedemptionStatus
import com.tgt.backpacktransactionsclient.transport.kafka.model.RegistryTransactionTO
import com.tgt.cronbeacon.kafka.model.CronEvent
import com.tgt.lists.atlas.api.transport.ListItemResponseTO
import com.tgt.lists.atlas.api.type.UserMetaData.Companion.toUserMetaData
import java.time.*
import java.util.*

@Suppress("UNCHECKED_CAST")
class RegistryDataProvider {

    val mapper = jacksonObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    fun getRecipient(recipientType: RecipientType, recipientRole: RecipientRole, firstName: String?, lastName: String?): RegistryRecipientTO {
        return RegistryRecipientTO(recipientType = recipientType, recipientRole = recipientRole, firstName = firstName, lastName = lastName)
    }

    fun getRecipientForCreateRegistry(recipientType: RecipientType, recipientRole: RecipientRole, firstName: String?, lastName: String?): Map<String?, String?> {
        return mapOf("recipient_type" to recipientType.toString(),
            "recipient_role" to recipientRole.toString(),
            "first_name" to firstName,
            "last_name" to lastName
        )
    }

    fun getRegistryEventForCreateRegistry(city: String?, state: String?, country: String?, eventDate: LocalDate): Map<String?, Any?> {
        return mapOf("city" to city,
            "state" to state,
            "country" to country,
            "event_date" to eventDate.toString()
        )
    }

    fun getRegistryEvent(city: String?, state: String?, country: String?, eventDate: LocalDate): RegistryEventTO {
        return RegistryEventTO(city = city, state = state, country = country, eventDate = eventDate)
    }

    fun getBabyTO(babyName: String?, babyGender: String?, firstChild: Boolean?): Map<String?, Any?> {
        return mapOf("baby_name" to babyName,
            "baby_gender" to babyGender,
            "first_child" to firstChild
        )
    }

    fun getBabyExtension(babyName: String?, babyGender: String?, firstChild: Boolean?): RegistryBabyTO {
        return RegistryBabyTO(babyName = babyName, babyGender = babyGender, firstChild = firstChild)
    }

    fun getRegistryMetaData(
        profileAddressId: UUID?,
        giftCardsEnabled: Boolean?,
        groupGiftEnabled: Boolean?,
        groupGiftAmount: String?,
        recipients: List<RegistryRecipientTO>?,
        event: RegistryEventTO?,
        babyRegistry: RegistryBabyTO?,
        guestRulesMetaData: RegistryGuestRuleMetaDataTO?,
        imageMetaData: RegistryImageMetaDataTO?
    ): String? {
        return RegistryMetaDataTO.toStringRegistryMetadata(RegistryMetaDataTO(profileAddressId, giftCardsEnabled,
            groupGiftEnabled, groupGiftAmount, recipients, event, babyRegistry, guestRulesMetaData, imageMetaData))
    }

    fun getGuestRulesMetaData(guestRulesMetaData: RegistryGuestRuleMetaDataTO?): String? {
        return RegistryMetaDataTO.toStringRegistryMetadata(RegistryMetaDataTO(guestRulesMetaData = guestRulesMetaData))
    }

    fun getMultiDeleteItemRequest(itemIdList: List<UUID>): RegistryItemMultiDeleteRequestTO {
        return RegistryItemMultiDeleteRequestTO(itemIdList)
    }

    fun getRegistryItemUpdateRequest(
        itemType: RegistryItemType,
        itemNote: String?,
        genericItemName: String?,
        externalProductUrl: String?,
        addedByRecipient: Boolean?,
        mostWantedFlag: Boolean?,
        requestedQuantity: Int?,
        purchasedQuantity: Int?
    ): RegistryItemUpdateRequestTO {
        return RegistryItemUpdateRequestTO(
            itemNote = itemNote,
            genericItemName = genericItemName,
            externalProductUrl = externalProductUrl,
            addedByRecipient = addedByRecipient,
            mostWantedFlag = mostWantedFlag,
            requestedQuantity = requestedQuantity,
            purchasedQuantity = purchasedQuantity
        )
    }

    fun getRegistryUpdateRequest(
        registryTitle: String?,
        shortDescription: String?,
        registryType: RegistryType?,
        giftCardsEnabled: Boolean?,
        groupGiftEnabled: Boolean?
    ): RegistryUpdateRequestTO {
        return RegistryUpdateRequestTO(
            registryTitle = registryTitle,
            shortDescription = shortDescription,
            giftCardsEnabled = giftCardsEnabled,
            groupGiftEnabled = groupGiftEnabled
        )
    }

    fun getCreateGuestRulesRequest(
        guestrules: Map<String, String>
    ): RegistryGuestRuleRequestTO {
        return RegistryGuestRuleRequestTO(guestrules)
    }

    fun getRegistryItemRequest(
        itemType: RegistryItemType,
        tcin: String?,
        genericItemName: String?,
        externalProductUrl: String?,
        externalRetailer: String?,
        externalProductPrice: String?,
        externalProductSize: String?,
        externalProductColor: String?,
        externalProductImageUrl: String?,
        addedByRecipient: Boolean?,
        requestedQuantity: Int = 1,
        purchasedQuantity: Int = 0,
        itemTitle: String?,
        itemNote: String?,
        agentId: String?,
        mostWantedFlag: Boolean? = false
    ): RegistryItemRequestTO {
        return RegistryItemRequestTO(
            itemType = itemType,
            tcin = tcin,
            genericItemName = genericItemName,
            externalProductUrl = externalProductUrl,
            externalRetailer = externalRetailer,
            externalProductPrice = externalProductPrice,
            externalProductSize = externalProductSize,
            externalProductColor = externalProductColor,
            externalProductImageUrl = externalProductImageUrl,
            addedByRecipient = addedByRecipient,
            requestedQuantity = requestedQuantity,
            purchasedQuantity = purchasedQuantity,
            itemTitle = itemTitle,
            itemNote = itemNote,
            agentId = agentId,
            mostWantedFlag = mostWantedFlag
        )
    }

    fun getMultiAddItemRequest(
        items: List<RegistryItemRequestTO>
    ): RegistryItemMultiAddRequestTO {
        return RegistryItemMultiAddRequestTO(items)
    }

    fun getRegistryItem(
        listItemId: UUID,
        tcin: String?,
        channel: RegistryChannel,
        itemTitle: String?,
        itemType: RegistryItemType,
        itemRelationship: String?,
        externalProductSize: String?,
        externalProductColor: String?,
        metadata: Map<String, Any>?
    ): ListItemResponseTO {
        return ListItemResponseTO(listItemId = listItemId, tcin = tcin, itemTitle = itemTitle,
            channel = channel.toString(),
            itemRefId = populateItemRefId(itemType, tcin, itemTitle, externalProductSize, externalProductColor),
            itemType = itemType.toListItemType(), relationshipType = itemRelationship, addedTs = Instant.now().toString(),
            metadata = if (metadata != null) toUserMetaData(mapper.writeValueAsString(metadata)) else null)
    }

    fun getRegistryResponseTO(
        registryId: UUID,
        registryTitle: String,
        itemsCount: Int,
        registryStatus: RegistryStatus
    ): RegistryResponseTO {
        return RegistryResponseTO(registryId = registryId, channel = RegistryChannel.WEB, listType = "REGISTRY",
            registryTitle = registryTitle, shortDescription = null, agentId = null, addedTs = null, lastModifiedTs = null,
            registryItems = null, itemsCount = itemsCount, subChannel = RegistrySubChannel.KIOSK, profileAddressId = UUID.randomUUID(),
            registryType = RegistryType.BABY, giftCardsEnabled = true, groupGiftEnabled = false, registryStatus = registryStatus,
            recipients = null, event = null, babyRegistry = null, honeyFundItems = null)
    }

    fun getRegistryItemResponseTO(
        registryItemId: UUID,
        tcin: String,
        requestedQuantity: Int,
        purchasedQuantity: Int
    ): RegistryItemResponseTO {
        return RegistryItemResponseTO(registryItemId = registryItemId, itemType = null, channel = RegistryChannel.WEB,
            subChannel = RegistrySubChannel.KIOSK, tcin = tcin, requestedQuantity = requestedQuantity, purchasedQuantity = purchasedQuantity)
    }

    fun getImageRequest(
        imageInfo: RegistryImageInfoTO?
    ): RegistryImageRequestTO {
        return RegistryImageRequestTO(imageUrl = imageInfo?.imageUrl,
            imageId = imageInfo?.imageId,
            dimension = imageInfo?.dimension,
            type = imageInfo?.type,
            imageStatus = imageInfo?.imageStatus)
    }

    fun getUTCLocalDateTimeStamp(): String {
        return LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC).toString()
    }

    fun getRegistryCustomUrlRequest(
        customUrl: String
    ): RegistryCustomUrlRequestTO {
        return RegistryCustomUrlRequestTO(customUrl = customUrl)
    }

    fun getRegistryTransactionTO(
        registryId: UUID,
        tcin: String,
        purchasedQuantity: Int? = 1
    ): RegistryTransactionTO {
        val date = LocalDateTime.now()
        return RegistryTransactionTO(111L, "pos1", null, null,
            null, registryId, date, "PURCHASE", "123456", "POS", date,
            "pos", date, tcin, "123-45-1234", "1234654", purchasedQuantity!!, 112.9, 1375,
            "1111111", "amazon", "out of stock", "fn", "ln",
            true, false, true, "reta", "12345",
            true, RegistryChannel.WEB.value, RegistrySubChannel.TGTWEB.value)
    }

    fun getDefaultRegistryItemMetaData(): String? {
        return toStringRegistryItemMetaData(RegistryItemMetaDataTO(null, null, null,
            null, null, null, null, false, false,
            false, null, null, null, null))
    }

    fun getDefaultRegistryItemMetaDataMap(): Map<String, Any>? {
        return toUserMetaData(toStringRegistryItemMetaData(RegistryItemMetaDataTO(null, null, null,
            null, null, null, null, false, false,
            false, null, null, null, null)))?.metadata
    }

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
}
