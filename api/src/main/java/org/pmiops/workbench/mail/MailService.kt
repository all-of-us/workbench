package org.pmiops.workbench.mail

import com.google.api.services.directory.model.User
import javax.mail.MessagingException

interface MailService {

    @Throws(MessagingException::class)
    fun sendBetaAccessRequestEmail(userName: String)

    @Throws(MessagingException::class)
    fun sendWelcomeEmail(contactEmail: String, password: String, user: User)

    @Throws(MessagingException::class)
    fun sendBetaAccessCompleteEmail(contactEmail: String, username: String)
}
