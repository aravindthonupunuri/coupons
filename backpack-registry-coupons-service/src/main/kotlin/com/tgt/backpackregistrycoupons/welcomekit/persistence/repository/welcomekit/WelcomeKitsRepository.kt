package com.tgt.backpackregistrycoupons.welcomekit.persistence.repository.welcomekit

import com.tgt.backpackregistrycoupons.welcomekit.domain.model.WelcomeKits
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface WelcomeKitsRepository {

    fun save(coupons: List<WelcomeKits>): Flux<WelcomeKits>

    fun findByTcinInList(tcin: List<String>): Flux<WelcomeKits>

    fun existsByTcin(tcin: String): Mono<Boolean>
}
