package com.tgt.backpackregistrycoupons.domain.model

import com.tgt.backpackregistryclient.util.RegistryType
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Relation
import io.micronaut.data.model.DataType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Id
import javax.persistence.Table

@MappedEntity
@Table(name = "registry")
data class Registry(
    @Id
    @MappedProperty(type = DataType.OBJECT)
    @Column(name = "registry_id")
    val registryId: UUID,

    @Column(name = "registry_type")
    val registryType: RegistryType,

    @Column(name = "registry_status")
    val registryStatus: String,

    @Column(name = "registry_created_ts")
    val registryCreatedTs: LocalDateTime,

    @Column(name = "event_date")
    val eventDate: LocalDate,

    @Column(name = "coupon_assignment_complete")
    val couponAssignmentComplete: Boolean,

    @DateCreated
    @Column(name = "created_ts")
    var createdTs: LocalDateTime? = null,

    @DateCreated
    @Column(name = "updated_ts")
    var updatedTs: LocalDateTime? = null

) {

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "registry", cascade = [Relation.Cascade.ALL])
    var registryCoupons: Set<RegistryCoupons>? = null
}
