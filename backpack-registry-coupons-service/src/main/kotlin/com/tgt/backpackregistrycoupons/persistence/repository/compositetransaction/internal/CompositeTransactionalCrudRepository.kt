package com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction.internal

import com.tgt.backpackregistrycoupons.domain.model.Registry
import com.tgt.backpackregistrycoupons.domain.model.RegistryCoupons
import com.tgt.backpackregistrycoupons.persistence.repository.compositetransaction.CompositeTransactionalRepository
import com.tgt.lists.micronaut.persistence.ApplicationTransaction
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.time.LocalDateTime
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

    override fun assignCoupons(registryCoupons: List<RegistryCoupons>): Mono<Boolean> {
        logger.debug("Executing assignCoupons for registryCoupons $registryCoupons")
        val couponCodes = registryCoupons.map { it.couponCode!! }

        return nonBlockingExec {
            applicationTransaction.executeWrite {
                val registry_coupons_sql = "INSERT INTO registry_coupons (coupon_code, registry_id, coupon_type, coupon_redemption_status, coupon_issue_date, coupon_expiry_date, created_ts, updated_ts) VALUES (?,?,?,?,?,?,?,?)"
                val coupons_sql = "DELETE FROM coupons WHERE coupon_code = ?"

                var result: Pair<List<Int>, List<Int>> = Pair(emptyList(), emptyList())
                applicationTransaction.jdbcRepositoryOperations().prepareStatement(registry_coupons_sql) { statement ->
                    for (registryCoupon in registryCoupons) {
                        statement.setObject(1, registryCoupon.couponCode)
                        statement.setObject(2, registryCoupon.registry?.registryId)
                        statement.setObject(3, registryCoupon.couponType.name)
                        statement.setObject(4, registryCoupon.couponRedemptionStatus?.name)
                        statement.setObject(5, registryCoupon.couponIssueDate)
                        statement.setObject(6, registryCoupon.couponExpiryDate)
                        statement.setObject(7, LocalDateTime.now())
                        statement.setObject(8, LocalDateTime.now())
                        statement.addBatch()
                    }

                    val updateCount = statement.executeBatch()
                    result = result.copy(first = updateCount.toList())
                }
                if (result.first.none { it == 0 }) {
                    applicationTransaction.jdbcRepositoryOperations().prepareStatement(coupons_sql) { statement ->
                        for (couponCode in couponCodes) {
                            statement.setObject(1, couponCode)
                            statement.addBatch()
                        }

                        val updateCount = statement.executeBatch()
                        result = result.copy(second = updateCount.toList())
                    }
                    if (result.first.any { it == 0 }) {
                        // throw exception to rollback transaction
                        // - see executeWrite in io.micronaut.transaction.support.AbstractSynchronousTransactionManager
                        throw RuntimeException("Exception deleting coupon after being assigned to a registry with registryId $coupons_sql")
                    }
                } else {
                    // throw exception to rollback transaction
                    // - see executeWrite in io.micronaut.transaction.support.AbstractSynchronousTransactionManager
                    throw RuntimeException("Exception inserting RegistryCoupons for registryId $registry_coupons_sql")
                }
                true
            }
        }
    }

    private fun <T> nonBlockingExec(block: () -> T): Mono<T> {
        logger.info("Using nonBlockingExec(...)")
        return Mono.fromCallable(block)
            .subscribeOn(applicationTransaction.jdbcTransactionScheduler())
    }
}
