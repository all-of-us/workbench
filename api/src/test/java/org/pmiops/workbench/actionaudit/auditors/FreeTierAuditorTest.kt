package org.pmiops.workbench.actionaudit.auditors

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.actionaudit.targetproperties.ProfileTargetProperty
import org.pmiops.workbench.db.model.DbUser
import org.springframework.test.context.junit4.SpringRunner
import java.time.Clock
import java.time.Instant
import javax.inject.Provider

@RunWith(SpringRunner::class)
class FreeTierAuditorTest {

    private val mockUserProvider = mock<Provider<DbUser>>()
    private val mockActionAuditService = mock<ActionAuditService>()
    private val mockClock = mock<Clock>()
    private val mockActionIdProvider = mock<Provider<String>>()

    private var freeTierAuditAdapter: FreeTierAuditor? = null
    private var adminUser: DbUser? = null

    @Before
    fun setUp() {
        adminUser = DbUser()
                .apply { userId = ADMIN_USER_ID }
                .apply { username = ADMIN_USER_EMAIL }

        freeTierAuditAdapter = FreeTierAuditorImpl(
                userProvider = mockUserProvider,
                actionAuditService = mockActionAuditService,
                clock = mockClock,
                actionIdProvider = mockActionIdProvider)
        whenever(mockClock.millis()).thenReturn(Y2K_EPOCH_MILLIS)
        whenever(mockUserProvider.get()).thenReturn(adminUser)
        whenever(mockActionIdProvider.get()).thenReturn(ACTION_ID)
    }

    @Test
    fun testSetFreeTierDollarQuota_initial() {
        freeTierAuditAdapter!!.fireFreeTierDollarQuotaAction(USER_ID, null, 123.45)
        argumentCaptor<ActionAuditEvent>().apply {
            verify(mockActionAuditService).send(capture())
            val eventSent = firstValue

            assertThat(eventSent.actionId).isEqualTo(ACTION_ID)
            assertThat(eventSent.timestamp).isEqualTo(Y2K_EPOCH_MILLIS)
            assertThat(eventSent.actionType).isEqualTo(ActionType.FREE_TIER_DOLLAR_OVERRIDE)
            assertThat(eventSent.agentType).isEqualTo(AgentType.ADMINISTRATOR)
            assertThat(eventSent.agentIdMaybe).isEqualTo(ADMIN_USER_ID)
            assertThat(eventSent.agentEmailMaybe).isEqualTo(ADMIN_USER_EMAIL)
            assertThat(eventSent.targetType).isEqualTo(TargetType.USER)
            assertThat(eventSent.targetIdMaybe).isEqualTo(USER_ID)
            assertThat(eventSent.targetPropertyMaybe).isEqualTo(ProfileTargetProperty.FREE_TIER_DOLLAR_QUOTA.name)
            assertThat(eventSent.previousValueMaybe).isNull()
            assertThat(eventSent.newValueMaybe).isEqualTo("123.45")
        }
    }

    @Test
    fun testSetFreeTierDollarQuota_change() {
        freeTierAuditAdapter!!.fireFreeTierDollarQuotaAction(USER_ID, 123.45, 500.0)
        argumentCaptor<ActionAuditEvent>().apply {
            verify(mockActionAuditService).send(capture())
            val eventSent = firstValue

            assertThat(eventSent.actionId).isEqualTo(ACTION_ID)
            assertThat(eventSent.timestamp).isEqualTo(Y2K_EPOCH_MILLIS)
            assertThat(eventSent.actionType).isEqualTo(ActionType.FREE_TIER_DOLLAR_OVERRIDE)
            assertThat(eventSent.agentType).isEqualTo(AgentType.ADMINISTRATOR)
            assertThat(eventSent.agentIdMaybe).isEqualTo(ADMIN_USER_ID)
            assertThat(eventSent.agentEmailMaybe).isEqualTo(ADMIN_USER_EMAIL)
            assertThat(eventSent.targetType).isEqualTo(TargetType.USER)
            assertThat(eventSent.targetIdMaybe).isEqualTo(USER_ID)
            assertThat(eventSent.targetPropertyMaybe).isEqualTo(ProfileTargetProperty.FREE_TIER_DOLLAR_QUOTA.name)
            assertThat(eventSent.previousValueMaybe).isEqualTo("123.45")
            assertThat(eventSent.newValueMaybe).isEqualTo("500.0")
        }
    }

    companion object {
        private const val USER_ID = 101L
        private const val USER_EMAIL = "a@b.com"
        private const val ADMIN_USER_ID = 202L
        private const val ADMIN_USER_EMAIL = "b@c.com"
        private val Y2K_EPOCH_MILLIS = Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli()
        private const val ACTION_ID = "58cbae08-447f-499f-95b9-7bdedc955f4d"
    }
}
