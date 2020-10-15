package com.tgt.backpackregistrycoupons.persistence.repository.registrycoupons

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import io.micronaut.data.annotation.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

interface RegistryCouponsRepository {

    fun saveAll(registryCoupons: List<RegistryCoupons>): Flux<RegistryCoupons>

    fun findByIdRegistryId(registryId: UUID): Flux<RegistryCoupons>

    @Query("""DELETE FROM registry_coupons WHERE registry_id=uuid(:registryId)""")
    fun deleteByIdRegistryId(registryId: UUID): Mono<Int>

    @Query("""UPDATE registry_coupons SET event_date = (:eventDate) WHERE registry_id=uuid(:registryId)""")
    fun updateByRegistryId(registryId: UUID, eventDate: LocalDateTime): Mono<Int>
}
