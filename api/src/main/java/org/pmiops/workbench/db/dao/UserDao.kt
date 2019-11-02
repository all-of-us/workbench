package org.pmiops.workbench.db.dao

import org.pmiops.workbench.db.model.User
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface UserDao : CrudRepository<User, Long> {

    fun findUserByEmail(email: String): User

    fun findUserByUserId(userId: Long): User

    fun findUserByContactEmail(contactEmail: String): List<User>

    @Query("SELECT user FROM User user")
    fun findUsers(): List<User>

    /** Returns the user with their authorities loaded.  */
    @Query("SELECT user FROM User user LEFT JOIN FETCH user.authorities WHERE user.userId = :id")
    fun findUserWithAuthorities(@Param("id") id: Long): User

    /** Returns the user with the page visits and authorities loaded.  */
    @Query("SELECT user FROM User user LEFT JOIN FETCH user.authorities LEFT JOIN FETCH user.pageVisits WHERE user.userId = :id")
    fun findUserWithAuthoritiesAndPageVisits(@Param("id") id: Long): User

    /** Find users matching the user's name or email  */
    @Query("SELECT user FROM User user WHERE user.dataAccessLevel IN :dals AND ( lower(user.email) LIKE lower(concat('%', :term, '%')) OR lower(user.familyName) LIKE lower(concat('%', :term, '%')) OR lower(user.givenName) LIKE lower(concat('%', :term, '%')) )")
    fun findUsersByDataAccessLevelsAndSearchString(
            @Param("dals") dataAccessLevels: List<Short>, @Param("term") term: String, sort: Sort): List<User>
}
