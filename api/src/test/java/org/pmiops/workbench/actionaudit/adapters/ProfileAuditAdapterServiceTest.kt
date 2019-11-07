package org.pmiops.workbench.actionaudit.adapters

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
class ProfileAuditAdapterServiceTest {

    private val mockUserProvider = mock<Provider<DbUser>>()
    private val mockActionAuditService = mock<ActionAuditService>()
    private val mockClock = mock<Clock>()
    private val mockActionIdProvider = mock<Provider<String>>()

    private var profileAuditAdapterService: ProfileAuditAdapterService? = null
    private var user: DbUser? = null

    @Before
    fun setUp() {
        user = DbUser()
        user?.userId = 1001
        user?.email = USER_EMAIL

        profileAuditAdapterService = ProfileAuditAdapterServiceImpl(
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
        profileAuditAdapterService!!.fireCreateAction(createdProfile)
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
        caltechAffiliation.institution = "Caltech"
        caltechAffiliation.role = "T.A."
        caltechAffiliation.nonAcademicAffiliation = NonAcademicAffiliation.COMMUNITY_SCIENTIST
        caltechAffiliation.other = "They are all fine houses."

        val mitAffiliation = InstitutionalAffiliation()
        mitAffiliation.institution = "MIT"
        mitAffiliation.role = "Professor"
        mitAffiliation.nonAcademicAffiliation = NonAcademicAffiliation.EDUCATIONAL_INSTITUTION

        val demographicSurvey = DemographicSurvey()
        demographicSurvey.disability = false
        demographicSurvey.ethnicity = Ethnicity.NOT_HISPANIC
        demographicSurvey.gender = listOf(Gender.PREFER_NO_ANSWER)
        demographicSurvey.yearOfBirth = BigDecimal.valueOf(1999)
        demographicSurvey.race = listOf(Race.PREFER_NO_ANSWER)
        demographicSurvey.education = Education.MASTER

        val createdProfile = Profile()
        createdProfile.username = "slim_shady"
        createdProfile.contactEmail = USER_EMAIL
        createdProfile.dataAccessLevel = DataAccessLevel.REGISTERED
        createdProfile.givenName = "Robert"
        createdProfile.familyName = "Paulson"
        createdProfile.phoneNumber = "867-5309"
        createdProfile.currentPosition = "Grad Student"
        createdProfile.organization = "Classified"
        createdProfile.disabled = false
        createdProfile.aboutYou = "Nobody in particular"
        createdProfile.areaOfResearch = "Aliens"
        createdProfile.institutionalAffiliations = listOf(caltechAffiliation, mitAffiliation)
        createdProfile.demographicSurvey = demographicSurvey
        return createdProfile
    }

    @Test
    fun testDeleteUserProfile() {
        profileAuditAdapterService!!.fireDeleteAction(USER_ID, USER_EMAIL)
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
