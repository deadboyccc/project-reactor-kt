package dev.dead.projectreactorkt.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

/**
 * R2DBC entity — NOT a JPA entity.
 * Annotations come from spring-data-relational, NOT jakarta.persistence.
 *
 * Key rules:
 *  - @Id must be nullable (null = new record, non-null = existing)
 *  - Use a data class for structural equality in tests
 *  - @Column maps field name → column name (optional if they match)
 *  - No @GeneratedValue needed — R2DBC infers auto-increment from a null @Id
 */
@Table("users")
data class User(

    @Id
    val id: Long? = null,

    @Column("name")
    val name: String,

    @Column("email")
    val email: String,

    @Column("age")
    val age: Int,

    /**
     * @CreatedDate requires auditing to be enabled via
     * @EnableR2dbcAuditing on a @Configuration class.
     * Without it, set a default manually like below.
     */
    @CreatedDate
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
