package dev.dead.projectreactorkt.repository

import dev.dead.projectreactorkt.model.User
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

/**
 * CoroutineCrudRepository — the fully coroutine-native Spring Data interface.
 *
 * Hierarchy of reactive repo interfaces (pick one):
 *
 *  ReactiveCrudRepository<T, ID>
 *    └─ returns Mono<T> / Flux<T>  — Project Reactor types
 *
 *  CoroutineCrudRepository<T, ID>   ← preferred in Kotlin
 *    └─ returns T? / Flow<T>        — native Kotlin coroutine types
 *       save(), findById() etc are `suspend` functions
 *
 * Spring auto-generates the implementation at runtime — no @Impl needed.
 */
@Repository
interface UserRepository : CoroutineCrudRepository<User, Long> {

    // ── Derived query methods ─────────────────────────────────────────────────
    // Spring parses the method name into SQL at startup time.

    /** Returns null if not found — suspend = one value or nothing */
    suspend fun findByEmail(email: String): User?

    /** Returns all matches as a cold Flow stream */
    fun findAllByName(name: String): Flow<User>

    /** Suspend + Boolean for existence checks */
    suspend fun existsByEmail(email: String): Boolean

    /** Count is scalar → suspend */
    suspend fun countByAge(age: Int): Long

    /** Delete returns Unit, not the deleted entity */
    suspend fun deleteByEmail(email: String)

    // ── Custom @Query methods ─────────────────────────────────────────────────
    // Use :paramName for named bind parameters (NOT ? positional).

    @Query("SELECT * FROM users WHERE age BETWEEN :min AND :max ORDER BY age")
    fun findAllByAgeBetween(min: Int, max: Int): Flow<User>

    @Query("SELECT * FROM users WHERE email LIKE :pattern")
    fun findAllByEmailLike(pattern: String): Flow<User>

    /** Modifying queries need @Modifying — returns rows affected */
    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun updateNameById(id: Long, name: String): Int
}
