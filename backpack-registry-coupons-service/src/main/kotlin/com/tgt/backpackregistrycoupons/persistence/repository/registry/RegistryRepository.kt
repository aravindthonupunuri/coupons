package com.tgt.backpackregistrycoupons.persistence.repository.registry

import com.tgt.backpackregistryclient.util.RegistryStatus
import com.tgt.backpackregistrycoupons.domain.model.Registry
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.*

interface RegistryRepository {

    fun save(registry: Registry): Mono<Registry>

    fun saveAll(registry: Set<Registry>): Flux<Registry>

    fun findByRegistryId(registryId: UUID): Mono<Registry>

    fun findByRegistryStatusAndCouponAssignmentComplete(registryStatus: RegistryStatus, couponAssignmentComplete: Boolean): Flux<Registry>

    @Join(value = "registryCoupons", type = Join.Type.LEFT_FETCH)
    fun getByRegistryId(registryId: UUID): Mono<Registry>

    @Query("UPDATE registry SET event_date = (:eventDate) WHERE registry_id=(:registryId)")
    fun updateRegistryEventDate(registryId: UUID, eventDate: LocalDate): Mono<Int>

    @Query("""UPDATE registry SET registry_status = (:registryStatus) WHERE registry_id=(:registryId)""")
    fun updateRegistryStatus(registryId: UUID, registryStatus: RegistryStatus): Mono<Int>

    @Query("""UPDATE registry SET coupon_assignment_complete = (:couponAssignmentComplete) WHERE registry_id=(:registryId)""")
    fun updateCouponAssignmentComplete(registryId: UUID, couponAssignmentComplete: Boolean): Mono<Int>
}
