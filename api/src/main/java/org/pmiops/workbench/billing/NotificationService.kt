package org.pmiops.workbench.billing

import org.pmiops.workbench.db.model.User

interface NotificationService {
    fun alertUser(user: User, msg: String)
}
