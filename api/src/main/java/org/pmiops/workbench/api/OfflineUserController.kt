package org.pmiops.workbench.api

import java.io.IOException
import java.sql.Timestamp
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.google.CloudResourceManagerService
import org.pmiops.workbench.model.DataAccessLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** Handles offline / cron-based API requests related to user management.  */
@RestController
class OfflineUserController @Autowired
constructor(
        private val cloudResourceManagerService: CloudResourceManagerService, private val userService: UserService) : OfflineUserApiDelegate {

    private fun timestampsEqual(a: Timestamp?, b: Timestamp?): Boolean {
        return a?.equals(b)
                ?: (b?.equals(a) ?: (a === b))
    }

    /**
     * Updates moodle information for all users in the database.
     *
     *
     * This API method is called by a cron job and is not part of our normal user-facing surface.
     */
    fun bulkSyncComplianceTrainingStatus(): ResponseEntity<Void> {
        var errorCount = 0
        var userCount = 0
        var changeCount = 0
        var accessLevelChangeCount = 0

        for (user in userService.allUsers) {
            userCount++
            try {
                val oldTime = user.complianceTrainingCompletionTime
                val oldLevel = user.dataAccessLevelEnum

                val updatedUser = userService.syncComplianceTrainingStatus(user)

                val newTime = updatedUser.complianceTrainingCompletionTime
                val newLevel = updatedUser.dataAccessLevelEnum

                if (!timestampsEqual(newTime, oldTime)) {
                    log.info(
                            String.format(
                                    "Compliance training completion changed for user %s. Old %s, new %s",
                                    user.email, oldTime, newTime))
                    changeCount++
                }
                if (oldLevel !== newLevel) {
                    log.info(
                            String.format(
                                    "Data access level changed for user %s. Old %s, new %s",
                                    user.email, oldLevel.toString(), newLevel.toString()))
                    accessLevelChangeCount++
                }
            } catch (e: org.pmiops.workbench.moodle.ApiException) {
                errorCount++
                log.severe(
                        String.format(
                                "Error syncing compliance training status for user %s: %s",
                                user.email, e.getMessage()))
            } catch (e: NotFoundException) {
                errorCount++
                log.severe(String.format("Error syncing compliance training status for user %s: %s", user.email, e.getMessage()))
            }

        }

        log.info(
                String.format(
                        "Checked %d users, updated %d completion times, updated %d access levels",
                        userCount, changeCount, accessLevelChangeCount))

        if (errorCount > 0) {
            throw ServerErrorException(
                    String.format("%d errors encountered during compliance training sync", errorCount))
        }

        return ResponseEntity.noContent().build()
    }

    /**
     * Updates eRA Commons information for all users in the database.
     *
     *
     * This API method is called by a cron job and is not part of our normal user-facing surface.
     */
    fun bulkSyncEraCommonsStatus(): ResponseEntity<Void> {
        var errorCount = 0
        var userCount = 0
        var changeCount = 0
        var accessLevelChangeCount = 0

        for (user in userService.allUsers) {
            userCount++
            try {
                val oldTime = user.eraCommonsCompletionTime
                val oldLevel = user.dataAccessLevelEnum

                val updatedUser = userService.syncEraCommonsStatusUsingImpersonation(user)

                val newTime = updatedUser.eraCommonsCompletionTime
                val newLevel = user.dataAccessLevelEnum

                if (!timestampsEqual(newTime, oldTime)) {
                    log.info(
                            String.format(
                                    "eRA Commons completion changed for user %s. Old %s, new %s",
                                    user.email, oldTime, newTime))
                    changeCount++
                }
                if (oldLevel !== newLevel) {
                    log.info(
                            String.format(
                                    "Data access level changed for user %s. Old %s, new %s",
                                    user.email, oldLevel.toString(), newLevel.toString()))
                    accessLevelChangeCount++
                }
            } catch (e: org.pmiops.workbench.firecloud.ApiException) {
                errorCount++
                log.severe(
                        String.format(
                                "Error syncing eRA Commons status for user %s: %s",
                                user.email, e.getMessage()))
            } catch (e: IOException) {
                errorCount++
                log.severe(
                        String.format(
                                "Error fetching impersonated creds for user %s: %s",
                                user.email, e.message))
            }

        }

        log.info(
                String.format(
                        "Checked %d users, updated %d completion times, updated %d access levels",
                        userCount, changeCount, accessLevelChangeCount))

        if (errorCount > 0) {
            throw ServerErrorException(
                    String.format("%d errors encountered during eRA Commons sync", errorCount))
        }
        return ResponseEntity.noContent().build()
    }

    /**
     * Updates 2FA information for all users in the database.
     *
     *
     * This API method is called by a cron job and is not part of our normal user-facing surface.
     */
    fun bulkSyncTwoFactorAuthStatus(): ResponseEntity<Void> {
        var errorCount = 0
        var userCount = 0
        var changeCount = 0
        var accessLevelChangeCount = 0

        for (user in userService.allUsers) {
            userCount++
            try {
                val oldTime = user.twoFactorAuthCompletionTime
                val oldLevel = user.dataAccessLevelEnum

                val updatedUser = userService.syncTwoFactorAuthStatus(user)

                val newTime = updatedUser.twoFactorAuthCompletionTime
                val newLevel = user.dataAccessLevelEnum

                if (!timestampsEqual(newTime, oldTime)) {
                    log.info(
                            String.format(
                                    "Two-factor auth completion changed for user %s. Old %s, new %s",
                                    user.email, oldTime, newTime))
                    changeCount++
                }
                if (oldLevel !== newLevel) {
                    log.info(
                            String.format(
                                    "Data access level changed for user %s. Old %s, new %s",
                                    user.email, oldLevel.toString(), newLevel.toString()))
                    accessLevelChangeCount++
                }
            } catch (e: Exception) {
                errorCount++
                log.severe(
                        String.format(
                                "Error syncing two-factor auth status for user %s: %s",
                                user.email, e.message))
            }

        }

        log.info(
                String.format(
                        "Checked %d users, updated %d completion times, updated %d access levels",
                        userCount, changeCount, accessLevelChangeCount))

        if (errorCount > 0) {
            throw ServerErrorException(
                    String.format("%d errors encountered during two-factor auth sync", errorCount))
        }
        return ResponseEntity.noContent().build()
    }

    /**
     * Audits GCP access for all users in the database.
     *
     *
     * This API method is called by a cron job and is not part of our normal user-facing surface.
     */
    fun bulkAuditProjectAccess(): ResponseEntity<Void> {
        for (user in userService.allUsers) {
            // TODO(RW-2062): Move to using the gcloud api for list all resources when it is available.
            val unauthorizedLogs = cloudResourceManagerService.getAllProjectsForUser(user).stream()
                    .filter { project -> !WHITELISTED_ORG_IDS.contains(project.parent.id) }
                    .map { project -> project.name + " in organization " + project.parent.id }
                    .collect<List<String>, Any>(Collectors.toList())
            if (unauthorizedLogs.size > 0) {
                log.log(
                        Level.WARNING,
                        "User "
                                + user.email
                                + " has access to projects: "
                                + unauthorizedLogs.joinToString(", "))
            }
        }
        return ResponseEntity.noContent().build()
    }

    companion object {
        private val log = Logger.getLogger(OfflineUserController::class.java.name)
        private val WHITELISTED_ORG_IDS = Arrays.asList(
                "400176686919", // test.firecloud.org
                "386193000800", // firecloud.org
                "394551486437" // pmi-ops.org
        )
    }
}
