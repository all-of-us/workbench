package org.pmiops.workbench.api

import com.google.common.annotations.VisibleForTesting
import java.util.function.Function
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.model.CdrVersionListResponse
import org.pmiops.workbench.model.DataAccessLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CdrVersionsController @Autowired
internal constructor(private val cdrVersionService: CdrVersionService, private var userProvider: Provider<User>?) : CdrVersionsApiDelegate {

    // TODO: Consider filtering this based on what is currently instantiated as a data source. Newly
    // added CDR versions will not function until a server restart.
    // TODO: consider different default CDR versions for different access levels
    val cdrVersions: ResponseEntity<CdrVersionListResponse>
        get() {
            val accessLevel = userProvider!!.get().dataAccessLevelEnum
            val cdrVersions = cdrVersionService.findAuthorizedCdrVersions(accessLevel)
            if (cdrVersions.isEmpty()) {
                throw ForbiddenException("User does not have access to any CDR versions")
            }
            val defaultVersions = cdrVersions.stream()
                    .filter { v -> v.isDefault }
                    .map { it.cdrVersionId }
                    .collect<List<Long>, Any>(Collectors.toList())
            if (defaultVersions.isEmpty()) {
                throw ForbiddenException("User does not have access to a default CDR version")
            }
            if (defaultVersions.size > 1) {
                log.severe(
                        String.format(
                                "Found multiple (%d) default CDR versions, picking one", defaultVersions.size))
            }
            return ResponseEntity.ok(
                    CdrVersionListResponse()
                            .items(cdrVersions.stream().map(TO_CLIENT_CDR_VERSION).collect(Collectors.toList<T>()))
                            .defaultCdrVersionId(java.lang.Long.toString(defaultVersions[0])))
        }

    @VisibleForTesting
    internal fun setUserProvider(userProvider: Provider<User>) {
        this.userProvider = userProvider
    }

    companion object {
        private val log = Logger.getLogger(CdrVersionsController::class.java.name)

        @VisibleForTesting
        internal val TO_CLIENT_CDR_VERSION = { cdrVersion: CdrVersion ->
            org.pmiops.workbench.model.CdrVersion()
                    .cdrVersionId(cdrVersion.cdrVersionId.toString())
                    .creationTime(cdrVersion.creationTime.time)
                    .dataAccessLevel(cdrVersion.dataAccessLevelEnum)
                    .archivalStatus(cdrVersion.archivalStatusEnum)
                    .name(cdrVersion.name)
        }
    }
}
