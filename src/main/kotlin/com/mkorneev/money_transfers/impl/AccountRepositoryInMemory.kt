package com.mkorneev.money_transfers.impl

import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.either.monad.flatMap
import arrow.data.extensions.list.traverse.map
import arrow.data.extensions.list.traverse.sequence
import arrow.data.extensions.listk.monad.map
import com.mkorneev.money_transfers.model.*
import org.slf4j.LoggerFactory
import scala.concurrent.stm.Ref
import scala.concurrent.stm.japi.STM

data class Update<E>(val id: AccountNumber, val f: (Account) -> Either<E, Account>)
data class RefUpdate<A, E>(val ref: Ref.View<A>, val f: (A) -> Either<E, A>)
data class Replacement<A>(val ref: Ref.View<A>, val value: A)

class AccountRepositoryInMemory : AccountRepository() {
    private val accounts: MutableMap<AccountNumber, Ref.View<Account>> = HashMap()

    val logger = LoggerFactory.getLogger(AccountRepositoryInMemory::class.java)

    override fun query(id: AccountNumber): Try<Either<NotFound<AccountNumber>, Account>> =
            Success(accounts[id].rightIfNotNull { NotFound(id) }.map { it.get() })

    override fun create(id: AccountNumber, value: Account): Either<DuplicateId, Account> = synchronized(accounts) {
        if (accounts.containsKey(id)) {
            DuplicateId.left()
        } else {
            accounts[id] = STM.newRef(value)
            value.right()
        }
    }

    override fun <E> transform1(id: AccountNumber, f: (Account) -> Either<E, Account>):
            Either<Either<NotFound<AccountNumber>, E>, Account> {
        return accounts[id]
                .rightIfNotNull { NotFound(id).left() }
                .flatMap { ref ->
                    val callable = {
                        val result = f(ref.get())
                        result.map { b -> ref.set(b); b }
                    }

                    STM.atomic(callable).mapLeft { it.right() }
                }
    }

    override fun <E> transform(updates: List<Update<E>>):
            Either<Either<NotFound<AccountNumber>, E>, List<Account>> {
        return updates
                .map { update ->
                    accounts[update.id]
                            .rightIfNotNull { NotFound(update.id).left() }
                            .map { RefUpdate(it, update.f) }
                }
                .sequence(Either.applicative())
                .flatMap { refs ->
                    val callable = {
                        STM.afterCommit { logger.info("STM Commit") }
                        STM.afterRollback { logger.info("STM Rollback") }
                        STM.afterCompletion { logger.debug("STM Complete") }

                        val results = refs
                                .map { update ->
                                    (update.f)(update.ref.get())
                                            .map { Replacement(update.ref, it) }
                                }
                                .sequence(Either.applicative()).fix()

                        results.map { resultList ->
                            resultList.map { replacement ->
                                replacement.ref.set(replacement.value)
                                replacement.value
                            }
                        }
                    }

                    STM.atomic(callable).mapLeft { it.right() }
                }
    }

    override fun exists(id: AccountNumber): Try<Boolean> = Success(accounts.containsKey(id))
}
