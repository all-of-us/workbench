package org.pmiops.workbench.actionaudit.auditors

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever

import java.time.Clock
import java.time.Instant
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.DemographicSurvey
import org.pmiops.workbench.model.Education
import org.pmiops.workbench.model.Ethnicity
import org.pmiops.workbench.model.Gender
import org.pmiops.workbench.model.InstitutionalAffiliation
import org.pmiops.workbench.model.NonAcademicAffiliation
import org.pmiops.workbench.model.Profile
import org.pmiops.workbench.model.Race
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal

@RunWith(SpringRunner::class)
class ProfileAuditorTest {

    private val mockUserProvider = mock<Provider<DbUser>>()
    private val mockActionAuditService = mock<ActionAuditService>()
    private val mockClock = mock<Clock>()
    private val mockActionIdProvider = mock<Provider<String>>()

    private var profileAuditAdapter: ProfileAuditor? = null
    private var user: DbUser? = null

    @Before
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
            assertThat(sentEvents).hasSize(13)
            assertThat(sentEvents.all { it.actionType == ActionType.CREATE })
            assertThat(sentEvents.map { ActionAuditEvent::actionId }.distinct().count() == 1)
        }
    }

    private fun buildProfile(): Profile {
        val caltechAffiliation = InstitutionalAffiliation()
            .apply { institution = "Caltech" }
            .apply { role = "T.A." }
            .apply { nonAcademicAffiliation = NonAcademicAffiliation.COMMUNITY_SCIENTIST }
            .apply { other = "They are all fine houses." }

        val mitAffiliation = InstitutionalAffiliation()
            .apply { institution = "MIT" }
            .apply { role = "Professor" }
            .apply { nonAcademicAffiliation = NonAcademicAffiliation.EDUCATIONAL_INSTITUTION }

        val demographicSurvey1 = DemographicSurvey()
            .apply { disability = false }
            .apply { ethnicity = Ethnicity.NOT_HISPANIC }
            .apply { yearOfBirth = BigDecimal.valueOf(1999) }
            .apply { race = listOf(Race.PREFER_NO_ANSWER) }
            .apply { education = Education.MASTER }
            .apply { identifiesAsLgbtq = true }
            .apply { lgbtqIdentity = "gay" }

        return Profile()
            .apply { userId = 444 }
            .apply { username = "slim_shady" }
            .apply { contactEmail = USER_EMAIL }
            .apply { dataAccessLevel = DataAccessLevel.REGISTERED }
            .apply { givenName = "Robert" }
            .apply { familyName = "Paulson" }
            .apply { phoneNumber = "867-5309" }
            .apply { currentPosition = "Grad Student" }
            .apply { organization = "Classified" }
            .apply { disabled = false }
            .apply { aboutYou = "Nobody in particular" }
            .apply { areaOfResearch = "Aliens" }
            .apply { professionalUrl = "linkedin.com" }
            .apply { institutionalAffiliations = listOf(caltechAffiliation, mitAffiliation) }
            .apply { demographicSurvey = demographicSurvey1 }
    }

    @Test
    fun testDeleteUserProfile() {
        profileAuditAdapter!!.fireDeleteAction(USER_ID, USER_EMAIL)
        argumentCaptor<ActionAuditEvent>().apply {
            verify(mockActionAuditService).send(capture())
            val eventSent = firstValue

            assertThat(eventSent.targetType).isEqualTo(TargetType.PROFILE)
            assertThat(eventSent.agentType).isEqualTo(AgentType.USER)
            assertThat(eventSent.agentId).isEqualTo(USER_ID)
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
