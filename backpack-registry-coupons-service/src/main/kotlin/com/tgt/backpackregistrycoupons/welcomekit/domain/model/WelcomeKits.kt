package com.tgt.backpackregistrycoupons.welcomekit.domain.model

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Id
import javax.persistence.Table

@MappedEntity
@Table(name = "welcome_kits")
data class WelcomeKits(
    @Id
    @Column(name = "tcin")
    val tcin: String,

    @DateCreated
    @Column(name = "created_ts")
    var createdTs: LocalDateTime? = null,

    @DateUpdated
    @Column(name = "updated_ts")
    var updatedTs: LocalDateTime? = null
)
