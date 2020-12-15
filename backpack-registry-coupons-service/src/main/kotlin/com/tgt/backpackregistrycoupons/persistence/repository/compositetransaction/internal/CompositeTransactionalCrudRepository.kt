package com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction.internal

import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction.CompositeTransactionalRepository
import com.tgt.lists.micronaut.persistence.ApplicationTransaction
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.util.*

/*
Repository to handle multiple reactive database operations within a single database transaction.

The default micronaut declarative repositories don't allow multiple create/update/delete operations in a single database transaction.
whereas this custom repository allows to bundle multiple reactive create/update/delete operations in a single database transaction.
 */
@Repository
abstract class CompositeTransactionalCrudRepository(
    private val applicationTransaction: ApplicationTransaction
) : ReactiveStreamsCrudRepository<Registry, UUID>, CompositeTransactionalRepository {

    private val logger = KotlinLogging.logger {}

    override fun deleteRegistryCascaded(registryId: UUID): Mono<Pair<Int, Int>> {
        logger.debug("Executing deleteRegistryCascaded for accessId $registryId")

        return nonBlockingExec {
            applicationTransaction.executeWrite {
                val registry_coupons_sql = "DELETE FROM registry_coupons WHERE registry_id = ?"
                val registry_sql = "DELETE FROM registry WHERE registry_id = ?"

                var result: Pair<Int, Int> = Pair(0, 0)
                applicationTransaction.jdbcRepositoryOperations().prepareStatement(registry_sql) { statement ->
                    statement.setObject(1, registryId)
                    val updateCount = statement.executeUpdate()
                    result = result.copy(first = updateCount)
                }
                if (result.first > 0) {
                    applicationTransaction.jdbcRepositoryOperations().prepareStatement(registry_coupons_sql) { statement ->
                        statement.setObject(1, registryId)
                        val updateCount = statement.executeUpdate()
                        result = result.copy(second = updateCount)
                    }
                } else {
                    // throw exception to rollback transaction
                    // - see executeWrite in io.micronaut.transaction.support.AbstractSynchronousTransactionManager
                    throw RuntimeException("No records to delete for registry for registryId $registryId")
                }
                result
            }
        }
    }

    private fun <T> nonBlockingExec(block: () -> T): Mono<T> {
        logger.info("Using nonBlockingExec(...)")
        return Mono.fromCallable(block)
            .subscribeOn(applicationTransaction.jdbcTransactionScheduler())
    }
}
