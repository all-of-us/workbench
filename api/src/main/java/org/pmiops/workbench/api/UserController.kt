package org.pmiops.workbench.api

import com.google.common.collect.Lists
import java.util.Collections
import java.util.Optional
import java.util.function.Function
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.UserResponse
import org.pmiops.workbench.utils.PaginationToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController @Autowired
constructor(
        private val userProvider: Provider<User>,
        private val configProvider: Provider<WorkbenchConfig>,
        private val fireCloudService: FireCloudService,
        private val userService: UserService) : UserApiDelegate {

    fun user(
            term: String?, pageToken: String, size: Int?, sortOrder: String): ResponseEntity<UserResponse> {
        val response = UserResponse()
        response.setUsers(emptyList<T>())
        response.setNextPageToken("")

        if (null == term || term.isEmpty()) {
            return ResponseEntity.ok<UserResponse>(response)
        }

        val paginationToken: PaginationToken
        try {
            paginationToken = getPaginationTokenFromPageToken(pageToken)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body<UserResponse>(response)
        } catch (e: BadRequestException) {
            return ResponseEntity.badRequest().body<UserResponse>(response)
        }

        // See discussion on RW-2894. This may not be strictly necessary, especially if researchers
        // details will be published publicly, but it prevents arbitrary unregistered users from seeing
        // limited researcher profile details.
        val config = configProvider.get()
        if (!fireCloudService.isUserMemberOfGroup(
                        userProvider.get().email, config.firecloud.registeredDomainName)) {
            throw ForbiddenException("user search requires registered data access")
        }

        val direction = Optional.ofNullable<Direction>(Sort.Direction.fromStringOrNull(sortOrder)).orElse(Sort.Direction.ASC)
        val sort = Sort(Sort.Order(direction, DEFAULT_SORT_FIELD))

        // What we are really looking for here are users who have a FC account.
        // This should exist if they have signed in at least once
        val users = userService.findUsersBySearchString(term, sort).stream()
                .filter { user -> user.firstSignInTime != null }
                .collect<List<User>, Any>(Collectors.toList())

        val pageSize = Optional.ofNullable(size).orElse(DEFAULT_PAGE_SIZE)
        val pagedUsers = Lists.partition(users, pageSize)

        val pageOffset = java.lang.Long.valueOf(paginationToken.offset).toInt()

        if (pagedUsers.size == 0) {
            return ResponseEntity.ok<UserResponse>(response)
        }

        if (pageOffset < pagedUsers.size) {
            val hasNext = pageOffset < pagedUsers.size - 1
            if (hasNext) {
                response.setNextPageToken(PaginationToken.of(pageOffset + 1).toBase64())
            }
            val modelUsers = pagedUsers[pageOffset].stream()
                    .map(TO_USER_RESPONSE_USER)
                    .collect(Collectors.toList<Any>())
            response.setUsers(modelUsers)
        } else {
            log.warning(
                    String.format(
                            "User attempted autocomplete for a paged result that doesn't exist. Term: %s. Page: %d",
                            term, pageOffset))
            return ResponseEntity.badRequest().body<UserResponse>(response)
        }
        return ResponseEntity.ok<UserResponse>(response)
    }

    private fun getPaginationTokenFromPageToken(pageToken: String?): PaginationToken {
        return if (null == pageToken) PaginationToken.of(0) else PaginationToken.fromBase64(pageToken)
    }

    companion object {

        private val log = Logger.getLogger(UserController::class.java.name)
        private val DEFAULT_PAGE_SIZE = 10
        private val DEFAULT_SORT_FIELD = "email"
        private val TO_USER_RESPONSE_USER = { user ->
            val modelUser = org.pmiops.workbench.model.User()
            modelUser.setEmail(user.email)
            modelUser.setGivenName(user.givenName)
            modelUser.setFamilyName(user.familyName)
            modelUser
        }
    }
}
