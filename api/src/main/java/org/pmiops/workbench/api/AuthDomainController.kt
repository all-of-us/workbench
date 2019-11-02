package org.pmiops.workbench.api

import org.pmiops.workbench.annotations.AuthorityRequired
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.Authority
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.UpdateUserDisabledRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthDomainController @Autowired
internal constructor(
        private val fireCloudService: FireCloudService, private val userService: UserService, private val userDao: UserDao) : AuthDomainApiDelegate {

    @AuthorityRequired(Authority.DEVELOPER)
    fun createAuthDomain(groupName: String): ResponseEntity<EmptyResponse> {
        fireCloudService.createGroup(groupName)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    @AuthorityRequired(Authority.ACCESS_CONTROL_ADMIN)
    fun updateUserDisabledStatus(request: UpdateUserDisabledRequest): ResponseEntity<Void> {
        val user = userDao.findUserByEmail(request.getEmail())
        val previousDisabled = user.disabled
        val updatedUser = userService.setDisabledStatus(user.userId, request.getDisabled())
        userService.logAdminUserAction(
                user.userId, "updated user disabled state", previousDisabled, request.getDisabled())
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
