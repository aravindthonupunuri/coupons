package com.tgt.backpackregistrycoupons.welcomekit.service

import com.tgt.backpackregistrycoupons.welcomekit.domain.model.WelcomeKits
import com.tgt.backpackregistrycoupons.welcomekit.persistence.repository.welcomekit.WelcomeKitsRepository
import com.tgt.backpackregistrycoupons.welcomekit.transport.UploadWelcomeKitResponseTO
import mu.KotlinLogging
import reactor.core.publisher.Mono
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WelcomeKitUploadService(
    @Inject private val welcomeKitsRepository: WelcomeKitsRepository
) {
    private val logger = KotlinLogging.logger { WelcomeKitUploadService::class.java.name }

    fun uploadWelcomeKits(tcins: List<String>): Mono<UploadWelcomeKitResponseTO> {
        return if (tcins.isNullOrEmpty()) {
            logger.debug { "Empty welcome kits" }
            Mono.empty()
        } else {
            welcomeKitsRepository.findByTcinInList(tcins).collectList().flatMap { existingWelcomeKits ->
                val existingTcins: ArrayList<String> = arrayListOf()
                val newTcins: ArrayList<String> = arrayListOf()
                tcins.forEach { tcin ->
                    if (existingWelcomeKits.parallelStream().anyMatch { it.tcin == tcin }) {
                        existingTcins.add(tcin)
                    } else {
                        newTcins.add(tcin)
                    }
                }
                if (!newTcins.isNullOrEmpty()) {
                    welcomeKitsRepository.save(newTcins.map { WelcomeKits(it) }).collectList().map {
                        UploadWelcomeKitResponseTO(uploadedTcins = newTcins, existingTcins = existingTcins)
                    }
                } else {
                    Mono.just(UploadWelcomeKitResponseTO(existingTcins = existingTcins))
                }
            }
        }
    }
}
