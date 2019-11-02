package org.pmiops.workbench.mail

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*

import com.google.api.services.directory.model.User
import com.google.api.services.directory.model.UserEmail
import com.google.api.services.directory.model.UserName
import javax.mail.MessagingException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.google.CloudStorageServiceImpl
import org.pmiops.workbench.mandrill.ApiException
import org.pmiops.workbench.mandrill.api.MandrillApi
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses
import org.pmiops.workbench.test.Providers

class MailServiceImplTest {

    private var service: MailServiceImpl? = null

    @Mock
    private val cloudStorageService: CloudStorageServiceImpl? = null
    @Mock
    private val mandrillApi: MandrillApi? = null
    @Mock
    private val msgStatus: MandrillMessageStatus? = null
    @Rule
    var mockitoRule = MockitoJUnit.rule()

    @Before
    @Throws(ApiException::class)
    fun setUp() {
        val msgStatuses = MandrillMessageStatuses()
        msgStatuses.add(msgStatus)
        `when`(mandrillApi!!.send(any<T>())).thenReturn(msgStatuses)
        `when`(cloudStorageService!!.readMandrillApiKey()).thenReturn(API_KEY)
        `when`(cloudStorageService.getImageUrl(any())).thenReturn("test_img")

        service = MailServiceImpl(
                Providers.of<MandrillApi>(mandrillApi),
                Providers.of<CloudStorageService>(cloudStorageService),
                Providers.of(createWorkbenchConfig()))
    }

    @Test(expected = MessagingException::class)
    @Throws(MessagingException::class, ApiException::class)
    fun testSendWelcomeEmail_throwsMessagingException() {
        `when`(msgStatus!!.getRejectReason()).thenReturn("this was rejected")
        val user = createUser()
        service!!.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user)
        verify<Any>(mandrillApi, times(1)).send(any<T>())
    }

    @Test(expected = MessagingException::class)
    @Throws(MessagingException::class, ApiException::class)
    fun testSendWelcomeEmail_throwsApiException() {
        doThrow(ApiException::class.java).`when`(mandrillApi).send(any<T>())
        val user = createUser()
        service!!.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user)
        verify<Any>(mandrillApi, times(3)).send(any<T>())
    }

    @Test(expected = MessagingException::class)
    @Throws(MessagingException::class)
    fun testSendWelcomeEmail_invalidEmail() {
        val user = createUser()
        service!!.sendWelcomeEmail("Nota valid email", PASSWORD, user)
    }

    @Test
    @Throws(MessagingException::class, ApiException::class)
    fun testSendWelcomeEmail() {
        val user = createUser()
        service!!.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user)
        verify<Any>(mandrillApi, times(1)).send(any(MandrillApiKeyAndMessage::class.java!!))
    }

    private fun createUser(): User {
        return User()
                .setPrimaryEmail(PRIMARY_EMAIL)
                .setPassword(PASSWORD)
                .setName(UserName().setGivenName(GIVEN_NAME).setFamilyName(FAMILY_NAME))
                .setEmails(
                        UserEmail().setType("custom").setAddress(CONTACT_EMAIL).setCustomType("contact"))
                .setChangePasswordAtNextLogin(true)
    }

    private fun createWorkbenchConfig(): WorkbenchConfig {
        val workbenchConfig = WorkbenchConfig()
        workbenchConfig.mandrill = WorkbenchConfig.MandrillConfig()
        workbenchConfig.mandrill.fromEmail = "test-donotreply@fake-research-aou.org"
        workbenchConfig.mandrill.sendRetries = 3
        workbenchConfig.googleCloudStorageService = WorkbenchConfig.GoogleCloudStorageServiceConfig()
        workbenchConfig.googleCloudStorageService.credentialsBucketName = "test-bucket"
        workbenchConfig.admin = WorkbenchConfig.AdminConfig()
        workbenchConfig.admin.loginUrl = "http://localhost:4200/"
        return workbenchConfig
    }

    companion object {
        private val GIVEN_NAME = "Bob"
        private val FAMILY_NAME = "Bobberson"
        private val CONTACT_EMAIL = "bob@example.com"
        private val PASSWORD = "secretpassword"
        private val PRIMARY_EMAIL = "bob@researchallofus.org"
        private val API_KEY = "this-is-an-api-key"
    }
}
