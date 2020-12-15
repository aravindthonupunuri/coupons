// package com.tgt.backpackregistrycoupons.test.util
//
// import com.fasterxml.jackson.databind.PropertyNamingStrategy
// import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
// import com.tgt.backpackregistryclient.util.RegistryChannel
// import com.tgt.lists.atlas.api.domain.model.entity.*
// import com.tgt.lists.atlas.api.transport.ListGetAllResponseTO
// import com.tgt.lists.atlas.api.transport.ListItemRequestTO
// import com.tgt.lists.atlas.api.transport.ListItemResponseTO
// import com.tgt.lists.atlas.api.transport.ListResponseTO
// import com.tgt.lists.atlas.api.type.ItemType
// import com.tgt.lists.atlas.api.type.LIST_STATE
// import com.tgt.lists.atlas.api.type.UnitOfMeasure
// import com.tgt.lists.atlas.api.type.UserMetaData.Companion.toUserMetaData
// import java.time.Instant
// import java.time.LocalDate
// import java.time.LocalDateTime
// import java.time.ZoneOffset
// import java.util.*
//
// @Suppress("UNCHECKED_CAST")
// class ListDataProvider {
//    val mapper = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
//
//    fun getListItemRequestTO(itemType: ItemType, tcin: String): ListItemRequestTO {
//        return ListItemRequestTO(itemType = itemType, itemRefId = tcin, tcin = tcin, itemTitle = null)
//    }
//
//    fun getListResponse(
//        listId: UUID,
//        channel: String?,
//        listType: String?,
//        listTitle: String?,
//        metadata: Map<String, Any>?
//    ): ListResponseTO {
//        return ListResponseTO(
//            listId = listId,
//            channel = channel,
//            listType = listType,
//            listSubType = "WEDDING", // TODO get this from the caller
//            listState = LIST_STATE.ACTIVE, // TODO get this from the caller
//            listTitle = listTitle,
//            shortDescription = "shortDescription",
//            agentId = "1234",
//            defaultList = false,
//            metadata = if (metadata != null) toUserMetaData(mapper.writeValueAsString(metadata)) else null,
//            pendingListItems = null,
//            completedListItems = null,
//            addedTs = null,
//            lastModifiedTs = null,
//            maxCompletedItemsCount = 0,
//            maxPendingItemsCount = 100,
//            maxCompletedPageCount = null,
//            maxPendingPageCount = null
//        )
//    }
//
//    fun getAllListResponse(
//        listId: UUID?,
//        channel: String?
//    ): ListGetAllResponseTO {
//        return ListGetAllResponseTO(
//            listId = listId,
//            channel = channel,
//            listType = null,
//            listTitle = null,
//            defaultList = false,
//            shortDescription = "shortdescription",
//            agentId = "agentId",
//            metadata = null,
//            addedTs = null,
//            lastModifiedTs = null,
//            maxListsCount = 0,
//            pendingItemsCount = 0,
//            completedItemsCount = 0,
//            totalItemsCount = 0,
//            pendingItems = null,
//            completedItems = null
//        )
//    }
//
//    fun getListItemResponse(
//        listItemId: UUID?,
//        itemRefId: String,
//        tcin: String?,
//        itemNote: String?,
//        requestedQuantity: Int?
//    ): ListItemResponseTO {
//        return getListItemResponse(listItemId, itemRefId, tcin, itemNote, requestedQuantity, null)
//    }
//
//    fun getListItemResponse(
//        listItemId: UUID?,
//        itemRefId: String,
//        tcin: String?,
//        itemNote: String?,
//        requestedQuantity: Int?,
//        metadata: Map<String, Any>?
//    ): ListItemResponseTO {
//        return ListItemResponseTO(
//            listItemId = listItemId,
//            itemType = ItemType.TCIN,
//            itemRefId = itemRefId,
//            channel = RegistryChannel.WEB.toString(),
//            tcin = tcin,
//            itemTitle = null,
//            itemNote = itemNote,
//            requestedQuantity = requestedQuantity,
//            unitOfMeasure = UnitOfMeasure.EACHES,
//            metadata = if (metadata != null) toUserMetaData(mapper.writeValueAsString(metadata)) else null,
//            price = null,
//            listPrice = null,
//            offerCount = 0,
//            relationshipType = null,
//            itemState = null,
//            addedTs = null,
//            lastModifiedTs = null
//        )
//    }
//
//    fun getListResponse(
//        listId: UUID,
//        channel: String?,
//        listType: String?,
//        listTitle: String?,
//        metadata: Map<String, Any>?,
//        items: List<ListItemResponseTO>?
//    ): ListResponseTO {
//        return ListResponseTO(
//            listId = listId,
//            channel = channel,
//            listType = listType,
//            listSubType = "WEDDING", // TODO get this from the caller
//            listState = LIST_STATE.ACTIVE, // TODO get this from the caller
//            listTitle = listTitle,
//            shortDescription = "shortDescription",
//            agentId = "1234",
//            defaultList = false,
//            metadata = if (metadata != null) toUserMetaData(mapper.writeValueAsString(metadata)) else null,
//            pendingListItems = items,
//            completedListItems = null,
//            addedTs = null,
//            lastModifiedTs = null,
//            maxCompletedItemsCount = 0,
//            maxPendingItemsCount = 100,
//            maxCompletedPageCount = null,
//            maxPendingPageCount = null
//        )
//    }
//
//    fun getListResponseTO(listId: UUID, listType: String, listSubType: String, listTitle: String): ListResponseTO {
//        return ListResponseTO(listId = listId, channel = RegistryChannel.WEB.toString(), listType = listType, listSubType = listSubType, listState = LIST_STATE.ACTIVE, listTitle = listTitle,
//            shortDescription = "short$listId", agentId = null, metadata = null,
//            pendingListItems = emptyList(), completedListItems = emptyList(),
//            addedTs = null, lastModifiedTs = null, maxPendingItemsCount = 0, maxCompletedPageCount = 0,
//            maxPendingPageCount = null, maxCompletedItemsCount = null)
//    }
//
//    fun getList(listId: UUID, listTitle: String): ListGetAllResponseTO {
//        return ListGetAllResponseTO(listId = listId, listTitle = listTitle, listType = "SHOPPING", shortDescription = "test", metadata = null)
//    }
//
//    fun getListItem(listItemId: UUID, itemTitle: String): ListItemResponseTO {
//        return ListItemResponseTO(listItemId = listItemId, itemTitle = itemTitle, itemRefId = "" + itemTitle.hashCode())
//    }
//
//    fun createListEntity(listId: UUID, listTitle: String, listType: String, listSubtype: String, guestId: String, listMarker: String?): ListEntity {
//        return createListEntity(listId, listTitle, listType, listSubtype, guestId, listMarker, null)
//    }
//
//    fun createListEntity(listId: UUID, listTitle: String, listType: String, listSubtype: String, guestId: String, listMarker: String?, metadata: String?): ListEntity {
//        return ListEntity(id = listId, title = listTitle, type = listType, subtype = listSubtype, guestId = guestId,
//            marker = listMarker, metadata = metadata, expiration = LocalDate.now())
//    }
//
//    fun createListEntity(listId: UUID, listTitle: String, listType: String, listSubtype: String, guestId: String, listMarker: String, createdAt: Instant, updatedAt: Instant): ListEntity {
//        return createListEntity(listId, listTitle, listType, listSubtype, guestId, listMarker, createdAt, updatedAt, LIST_STATE.ACTIVE.value)
//    }
//
//    fun createListEntity(listId: UUID, listTitle: String, listType: String, listSubtype: String, guestId: String, listMarker: String, createdAt: Instant, updatedAt: Instant, state: String?): ListEntity {
//        return ListEntity(id = listId, title = listTitle, type = listType, subtype = listSubtype, guestId = guestId,
//            marker = listMarker, createdAt = createdAt, updatedAt = updatedAt, state = state, expiration = LocalDate.now())
//    }
//
//    fun createListItemEntity(listId: UUID, itemId: UUID, itemState: String, itemType: String, itemRefId: String, tcin: String?, itemTitle: String?, itemReqQty: Int?, itemNotes: String?): ListItemEntity {
//        return createListItemEntity(listId, itemId, itemState, itemType, itemRefId, tcin, itemTitle, itemReqQty, itemNotes, null, null)
//    }
//
//    fun createListItemEntity(listId: UUID, itemId: UUID, itemState: String, itemType: String, itemRefId: String, tcin: String?, itemTitle: String?, itemReqQty: Int?, itemNotes: String?, itemCreatedDate: Instant?, itemUpdatedDate: Instant?): ListItemEntity {
//        return createListItemEntity(listId, itemId, itemState, itemType, itemRefId, tcin, itemTitle, itemReqQty, null, itemNotes, itemCreatedDate, itemUpdatedDate)
//    }
//
//    fun createListItemEntity(listId: UUID, itemId: UUID, itemState: String, itemType: String, itemRefId: String, tcin: String?, itemTitle: String?, itemReqQty: Int?, itemPurchasedQty: Int?, itemNotes: String?): ListItemEntity {
//        return createListItemEntity(listId, itemId, itemState, itemType, itemRefId, tcin, itemTitle, itemReqQty, itemPurchasedQty, itemNotes, null, null)
//    }
//
//    fun createListItemEntity(listId: UUID, itemId: UUID, itemState: String, itemType: String, itemRefId: String, tcin: String?, itemTitle: String?, itemReqQty: Int?, itemPurchasedQty: Int?, itemNotes: String?, itemCreatedDate: Instant?, itemUpdatedDate: Instant?): ListItemEntity {
//        return createListItemEntity(listId, itemId, itemState, itemType, itemRefId, tcin, itemTitle, itemReqQty, itemPurchasedQty, itemNotes, itemCreatedDate, itemUpdatedDate, null)
//    }
//
//    fun createListItemEntity(listId: UUID, itemId: UUID, itemState: String, itemType: String, itemRefId: String, tcin: String?, itemTitle: String?, itemReqQty: Int?, itemPurchasedQty: Int?, itemNotes: String?, itemCreatedDate: Instant?, itemUpdatedDate: Instant?, itemMetadata: String?): ListItemEntity {
//        return ListItemEntity(id = listId, itemId = itemId, itemState = itemState, itemType = itemType, itemRefId = itemRefId,
//            itemTcin = tcin, itemTitle = itemTitle, itemReqQty = itemReqQty, itemQty = itemPurchasedQty, itemNotes = itemNotes,
//            itemCreatedAt = itemCreatedDate, itemUpdatedAt = itemUpdatedDate, itemMetadata = itemMetadata)
//    }
//
//    fun createGuestPreferenceEntity(guestId: String, listSortOrder: String?): GuestPreferenceEntity {
//        return GuestPreferenceEntity(guestId = guestId, listSortOrder = listSortOrder)
//    }
//
//    fun createGuestListEntity(guestId: String, type: String?, subtype: String?, marker: String?, id: UUID?, state: String?): GuestListEntity {
//        return GuestListEntity(guestId = guestId, type = type, subtype = subtype, marker = marker, id = id, state = state)
//    }
//
//    fun createListPreferenceEntity(listId: UUID, guestId: String, itemSortOrder: String?): ListPreferenceEntity {
//        return ListPreferenceEntity(listId = listId, guestId = guestId, itemSortOrder = itemSortOrder)
//    }
//
//    fun getListPreferenceEntity(listId: UUID, guestId: String): ListPreferenceEntity {
//        return ListPreferenceEntity(listId = listId, guestId = guestId)
//    }
//
//    fun createListItemExtEntity(listEntity: ListEntity, listItemEntity: ListItemEntity): ListItemExtEntity {
//        return ListItemExtEntity(id = listEntity.id, itemState = listItemEntity.itemState, itemId = listItemEntity.itemId,
//            itemType = listItemEntity.itemType, title = listEntity.title, type = listEntity.type, subtype = listEntity.subtype,
//            guestId = listEntity.guestId, marker = listEntity.marker, itemRefId = listItemEntity.itemRefId, itemTcin = listItemEntity.itemTcin,
//            description = listEntity.description, itemDesc = listItemEntity.itemDesc, itemTitle = listItemEntity.itemTitle,
//            itemCreatedAt = listItemEntity.itemCreatedAt, itemUpdatedAt = listItemEntity.itemUpdatedAt)
//    }
//
//    fun getLocalDateTimeInstant(): Instant {
//        return LocalDateTime.now().toInstant(ZoneOffset.UTC)
//    }
//
//    fun createGuestListEntity(guestId: String, listType: String, listId: UUID, listMarker: String?, listState: String?, listSubtype: String?): GuestListEntity {
//        return GuestListEntity(guestId = guestId, type = listType, subtype = listSubtype, marker = listMarker, id = listId, state = listState)
//    }
// }
