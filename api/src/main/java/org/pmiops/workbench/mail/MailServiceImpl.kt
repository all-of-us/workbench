package org.pmiops.workbench.mail

import com.google.api.services.directory.model.User
import com.google.common.collect.ImmutableMap
import com.google.common.io.Resources
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import javax.mail.MessagingException
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import org.apache.commons.lang3.text.StrSubstitutor
import org.apache.commons.lang3.tuple.ImmutablePair
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.mandrill.api.MandrillApi
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage
import org.pmiops.workbench.mandrill.model.MandrillMessage
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses
import org.pmiops.workbench.mandrill.model.RecipientAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MailServiceImpl @Autowired
constructor(
        private val mandrillApiProvider: Provider<MandrillApi>,
        private val cloudStorageServiceProvider: Provider<CloudStorageService>,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>) : MailService {
    private val BETA_ACCESS_TEXT = "A new user has requested beta access: "

    internal enum class Status {
        REJECTED,
        API_ERROR,
        SUCCESSFUL
    }

    @Throws(MessagingException::class)
    override fun sendBetaAccessRequestEmail(userName: String) {
        val workbenchConfig = workbenchConfigProvider.get()

        val msg = MandrillMessage()
        val toAddress = RecipientAddress()
        toAddress.setEmail(workbenchConfig.admin.adminIdVerification)
        msg.setTo(listOf<T>(toAddress))
        msg.setSubject("[Beta Access Request: " + workbenchConfig.server.shortName + "]: " + userName)
        msg.setHtml(BETA_ACCESS_TEXT + userName)
        msg.setFromEmail(workbenchConfig.mandrill.fromEmail)

        sendWithRetries(msg, "Beta Access submit notification")
    }

    @Throws(MessagingException::class)
    override fun sendWelcomeEmail(contactEmail: String, password: String, user: User) {
        try {
            val email = InternetAddress(contactEmail)
            email.validate()
        } catch (e: AddressException) {
            throw MessagingException("Email: $contactEmail is invalid.")
        }

        val msg = buildWelcomeMessage(contactEmail, password, user)
        sendWithRetries(msg, String.format("Welcome for %s", user.name))
    }

    @Throws(MessagingException::class)
    override fun sendBetaAccessCompleteEmail(contactEmail: String, username: String) {
        try {
            val email = InternetAddress(contactEmail)
            email.validate()
        } catch (e: AddressException) {
            throw MessagingException("Email: $contactEmail is invalid.")
        }

        val msg = buildBetaAccessCompleteMessage(contactEmail, username)
        sendWithRetries(msg, String.format("BetaAccess Complete for %s", contactEmail))
    }

    @Throws(MessagingException::class)
    private fun sendWithRetries(msg: MandrillMessage, description: String) {
        val apiKey = cloudStorageServiceProvider.get().readMandrillApiKey()
        var retries = workbenchConfigProvider.get().mandrill.sendRetries
        val keyAndMessage = MandrillApiKeyAndMessage()
        keyAndMessage.setKey(apiKey)
        keyAndMessage.setMessage(msg)
        do {
            retries--
            val attempt = trySend(keyAndMessage)
            val status = Status.valueOf(attempt.getLeft().toString())
            when (status) {
                MailServiceImpl.Status.API_ERROR -> {
                    log.log(
                            Level.WARNING,
                            String.format(
                                    "ApiException: Email '%s' not sent: %s",
                                    description, attempt.getRight().toString()))
                    if (retries == 0) {
                        log.log(
                                Level.SEVERE,
                                String.format(
                                        "ApiException: On Last Attempt! Email '%s' not sent: %s",
                                        description, attempt.getRight().toString()))
                        throw MessagingException("Sending email failed: " + attempt.getRight().toString())
                    }
                }

                MailServiceImpl.Status.REJECTED -> {
                    log.log(
                            Level.SEVERE,
                            String.format(
                                    "Messaging Exception: Email '%s' not sent: %s",
                                    description, attempt.getRight().toString()))
                    throw MessagingException("Sending email failed: " + attempt.getRight().toString())
                }

                MailServiceImpl.Status.SUCCESSFUL -> {
                    log.log(Level.INFO, String.format("Email '%s' was sent.", description))
                    return
                }

                else -> if (retries == 0) {
                    log.log(
                            Level.SEVERE, String.format("Email '%s' was not sent. Default case.", description))
                    throw MessagingException("Sending email failed: " + attempt.getRight().toString())
                }
            }
        } while (retries > 0)
    }

    @Throws(MessagingException::class)
    private fun buildWelcomeMessage(contactEmail: String, password: String, user: User): MandrillMessage {
        val msg = MandrillMessage()
        val toAddress = RecipientAddress()
        toAddress.setEmail(contactEmail)
        msg.setTo(listOf<T>(toAddress))
        try {
            val msgHtml = buildWelcomeEmailHtml(password, user)
            msg.html(msgHtml)
                    .subject("Your new All of Us Account")
                    .fromEmail(workbenchConfigProvider.get().mandrill.fromEmail)
            return msg
        } catch (e: IOException) {
            throw MessagingException("Error reading in email")
        }

    }

    @Throws(IOException::class)
    private fun buildWelcomeEmailHtml(password: String, user: User): String {
        val cloudStorageService = cloudStorageServiceProvider.get()
        val contentBuilder = StringBuilder()
        val emailContent = Resources.getResource(WELCOME_RESOURCE)
        Resources.readLines(emailContent, StandardCharsets.UTF_8)
                .forEach { s -> contentBuilder.append(s).append("\n") }
        val string = contentBuilder.toString()
        val replaceMap = ImmutableMap.Builder<String, String>()
                .put("USERNAME", user.primaryEmail)
                .put("PASSWORD", password)
                .put("URL", workbenchConfigProvider.get().admin.loginUrl)
                .put("HEADER_IMG", cloudStorageService.getImageUrl("all_of_us_logo.png"))
                .put("BULLET_1", cloudStorageService.getImageUrl("bullet_1.png"))
                .put("BULLET_2", cloudStorageService.getImageUrl("bullet_2.png"))
                .build()
        return StrSubstitutor(replaceMap).replace(string)
    }

    @Throws(MessagingException::class)
    private fun buildBetaAccessCompleteMessage(contactEmail: String, username: String): MandrillMessage {
        val msg = MandrillMessage()
        val toAddress = RecipientAddress()
        toAddress.setEmail(contactEmail)
        msg.setTo(listOf<T>(toAddress))
        try {
            val msgHtml = buildBetaAccessCompleteHtml(username)
            msg.html(msgHtml)
                    .subject("All of Us ID Verification Complete")
                    .fromEmail(workbenchConfigProvider.get().mandrill.fromEmail)
            return msg
        } catch (e: IOException) {
            throw MessagingException("Error reading in email")
        }

    }

    @Throws(IOException::class)
    private fun buildBetaAccessCompleteHtml(username: String): String {
        val cloudStorageService = cloudStorageServiceProvider.get()
        val contentBuilder = StringBuilder()
        val emailContent = Resources.getResource(BETA_ACCESS_RESOURCE)
        Resources.readLines(emailContent, StandardCharsets.UTF_8)
                .forEach { s -> contentBuilder.append(s).append("\n") }
        val string = contentBuilder.toString()
        val betaAccessReport: String
        val action: String

        betaAccessReport = "approved for use"
        action = ("login to the workbench via <a class=\"link\" href=\""
                + workbenchConfigProvider.get().admin.loginUrl
                + "\">"
                + workbenchConfigProvider.get().admin.loginUrl
                + "</a>")

        val replaceMap = ImmutableMap.Builder<String, String>()
                .put("ACTION", action)
                .put("BETA_ACCESS_REPORT", betaAccessReport)
                .put("HEADER_IMG", cloudStorageService.getImageUrl("all_of_us_logo.png"))
                .put("USERNAME", username)
                .build()
        return StrSubstitutor(replaceMap).replace(string)
    }

    private fun trySend(keyAndMessage: MandrillApiKeyAndMessage): ImmutablePair<Status, String> {
        try {
            val msgStatuses = mandrillApiProvider.get().send(keyAndMessage)
            for (msgStatus in msgStatuses) {
                if (msgStatus.getRejectReason() != null) {
                    return ImmutablePair<L, R>(Status.REJECTED, msgStatus.getRejectReason())
                }
            }
        } catch (e: Exception) {
            return ImmutablePair(Status.API_ERROR, e.toString())
        }

        return ImmutablePair(Status.SUCCESSFUL, "")
    }

    companion object {
        private val log = Logger.getLogger(MailServiceImpl::class.java.name)
        private val WELCOME_RESOURCE = "emails/welcomeemail/content.html"
        private val BETA_ACCESS_RESOURCE = "emails/betaaccessemail/content.html"
    }
}
