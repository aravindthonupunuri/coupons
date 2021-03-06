package com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction

import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import reactor.core.publisher.Mono
import java.util.*

interface CompositeTransactionalRepository {

    /*
    Deletes all Registry and associated RegistryCoupons for a given registryId
    (Required because cascade = [Relation.Cascade.ALL] not working
    */
    fun deleteRegistryCascaded(registryId: UUID): Mono<Pair<Int, Int>>

    fun assignCoupons(registryCoupons: List<RegistryCoupons>): Mono<Boolean>
}
