package org.pmiops.workbench.actionaudit.auditors

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import javax.inject.Provider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.access.AccessTierService
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.DemographicSurvey
import org.pmiops.workbench.model.Disability
import org.pmiops.workbench.model.Education
import org.pmiops.workbench.model.Ethnicity
import org.pmiops.workbench.model.InstitutionalRole
import org.pmiops.workbench.model.Profile
import org.pmiops.workbench.model.Race
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class ProfileAuditorTest {

    private val mockUserProvider = mock<Provider<DbUser>>()
    private val mockActionAuditService = mock<ActionAuditService>()
    private val mockClock = mock<Clock>()
    private val mockActionIdProvider = mock<Provider<String>>()

    private var profileAuditAdapter: ProfileAuditor? = null
    private var user: DbUser? = null

    @BeforeEach
    fun setUp() {
        user = DbUser()
                .apply { userId = 1001 }
                .apply { username = USER_EMAIL }

        profileAuditAdapter = ProfileAuditorImpl(
                userProvider = mockUserProvider,
                actionAuditService = mockActionAuditService,
                clock = mockClock,
                actionIdProvider = mockActionIdProvider)
        whenever(mockClock.millis()).thenReturn(Y2K_EPOCH_MILLIS)
        whenever(mockUserProvider.get()).thenReturn(user)
        whenever(mockActionIdProvider.get()).thenReturn(ACTION_ID)
    }

    @Test
    fun testCreateUserProfile() {
        val createdProfile = buildProfile()
        profileAuditAdapter!!.fireCreateAction(createdProfile)
        argumentCaptor<List<ActionAuditEvent>>().apply {
            verify(mockActionAuditService).send(capture())
            val sentEvents: List<ActionAuditEvent> = firstValue
            assertThat(sentEvents).hasSize(ProfileTargetProperty.values().size)
            assertThat(sentEvents.all { it.actionType == ActionType.CREATE })
            assertThat(sentEvents.map { ActionAuditEvent::actionId }.distinct().count() == 1)
        }
    }

    private fun buildProfile(): Profile {
        val caltechAffiliation = VerifiedInstitutionalAffiliation()
                .apply { institutionShortName = "Caltech" }
                .apply { institutionDisplayName = "California Institute of Technology" }
                .apply { institutionalRoleEnum = InstitutionalRole.ADMIN }

        val demographicSurvey1 = DemographicSurvey()
                .apply { disability = Disability.FALSE }
                .apply { ethnicity = Ethnicity.NOT_HISPANIC }
                .apply { yearOfBirth = BigDecimal.valueOf(1999) }
                .apply { race = listOf(Race.PREFER_NO_ANSWER) }
                .apply { education = Education.MASTER }
                .apply { identifiesAsLgbtq = true }
                .apply { lgbtqIdentity = "gay" }

        val addr = Address()
            .apply { streetAddress1 = "415 Main Street" }
            .apply { streetAddress2 = "7th floor" }
            .apply { zipCode = "12345" }
            .apply { city = "Cambridge" }
            .apply { state = "MA" }
            .apply { country = "USA" }

        return Profile()
                .apply { userId = 444 }
                .apply { username = "slim_shady" }
                .apply { contactEmail = USER_EMAIL }
                .apply { accessTierShortNames = listOf(AccessTierService.REGISTERED_TIER_SHORT_NAME) }
                .apply { givenName = "Robert" }
                .apply { familyName = "Paulson" }
                .apply { disabled = false }
                .apply { areaOfResearch = "Aliens" }
                .apply { professionalUrl = "linkedin.com" }
                .apply { verifiedInstitutionalAffiliation = caltechAffiliation }
                .apply { demographicSurvey = demographicSurvey1 }
                .apply { address = addr }
    }

    @Test
    fun testDeleteUserProfile() {
        profileAuditAdapter!!.fireDeleteAction(USER_ID, USER_EMAIL)
        argumentCaptor<ActionAuditEvent>().apply {
            verify(mockActionAuditService).send(capture())
            val eventSent = firstValue

            assertThat(eventSent.targetType).isEqualTo(TargetType.PROFILE)
            assertThat(eventSent.agentType).isEqualTo(AgentType.USER)
            assertThat(eventSent.agentIdMaybe).isEqualTo(USER_ID)
            assertThat(eventSent.targetIdMaybe).isEqualTo(USER_ID)
            assertThat(eventSent.actionType).isEqualTo(ActionType.DELETE)
            assertThat(eventSent.timestamp).isEqualTo(Y2K_EPOCH_MILLIS)
            assertThat(eventSent.targetPropertyMaybe).isNull()
            assertThat(eventSent.newValueMaybe).isNull()
            assertThat(eventSent.previousValueMaybe).isNull()
        }
    }

    companion object {

        private const val USER_ID = 101L
        private const val USER_EMAIL = "a@b.com"
        private val Y2K_EPOCH_MILLIS = Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli()
        private const val ACTION_ID = "58cbae08-447f-499f-95b9-7bdedc955f4d"
    }
}
