package org.pmiops.workbench.billing

import org.pmiops.workbench.db.model.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotificationLogger : NotificationService {

    override fun alertUser(user: User, msg: String) {
        logger.info("\nTO: " + user.email + " MSG: " + msg)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(BillingProjectBufferService::class.java)
    }
}
