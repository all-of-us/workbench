package org.pmiops.workbench.actionaudit.auditors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.ProfileTargetProperty;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;

public class ProfileAuditorTest {
  private static final long USER_ID = 101L;
  private static final String USER_EMAIL = "a@b.com";
  private static final long Y2K_EPOCH_MILLIS =
      Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli();
  private static final String ACTION_ID = "58cbae08-447f-499f-95b9-7bdedc955f4d";

  @Mock private ActionAuditService mockActionAuditService;
  @Mock private Clock mockClock;
  @Mock private Provider<String> mockActionIdProvider;

  private ProfileAuditorImpl profileAuditAdapter;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    DbUser user = new DbUser();
    user.setUserId(USER_ID);
    user.setUsername(USER_EMAIL);

    profileAuditAdapter =
        new ProfileAuditorImpl(mockActionAuditService, mockClock, mockActionIdProvider);
    when(mockClock.millis()).thenReturn(Y2K_EPOCH_MILLIS);
    when(mockActionIdProvider.get()).thenReturn(ACTION_ID);
  }

  @Test
  public void testCreateUserProfile() {
    Profile createdProfile = buildProfile();
    profileAuditAdapter.fireCreateAction(createdProfile);

    ArgumentCaptor<List<ActionAuditEvent>> captor = ArgumentCaptor.forClass(List.class);
    verify(mockActionAuditService).send(captor.capture());
    List<ActionAuditEvent> sentEvents = captor.getValue();
    assertEquals(ProfileTargetProperty.values().length, sentEvents.size());
    assertTrue(sentEvents.stream().allMatch(event -> event.actionType().equals(ActionType.CREATE)));
    assertEquals(1, sentEvents.stream().map(ActionAuditEvent::actionId).distinct().count());
  }

  private Profile buildProfile() {
    VerifiedInstitutionalAffiliation caltechAffiliation = new VerifiedInstitutionalAffiliation();
    caltechAffiliation.setInstitutionShortName("Caltech");
    caltechAffiliation.setInstitutionDisplayName("California Institute of Technology");
    caltechAffiliation.setInstitutionalRoleEnum(InstitutionalRole.ADMIN);

    DemographicSurvey demographicSurvey1 = new DemographicSurvey();
    demographicSurvey1.setDisability(Disability.FALSE);
    demographicSurvey1.setEthnicity(Ethnicity.NOT_HISPANIC);
    demographicSurvey1.setYearOfBirth(new BigDecimal(1999));
    demographicSurvey1.setRace(List.of(Race.PREFER_NO_ANSWER));
    demographicSurvey1.setEducation(Education.MASTER);
    demographicSurvey1.setLgbtqIdentity("gay");
    demographicSurvey1.setIdentifiesAsLgbtq(true);

    Address addr = new Address();
    addr.setStreetAddress1("415 Main Street");
    addr.setStreetAddress2("7th floor");
    addr.setZipCode("12345");
    addr.setCity("Cambridge");
    addr.setState("MA");
    addr.setCountry("USA");

    return new Profile()
        .userId(444L)
        .username("slim_shady")
        .contactEmail(USER_EMAIL)
        .accessTierShortNames(List.of(AccessTierService.REGISTERED_TIER_SHORT_NAME))
        .givenName("Robert")
        .familyName("Paulson")
        .areaOfResearch("Aliens")
        .professionalUrl("linkedin.com")
        .verifiedInstitutionalAffiliation(caltechAffiliation)
        .demographicSurvey(demographicSurvey1)
        .address(addr)
        .disabled(false);
  }

  @Test
  public void testDeleteUserProfile() {
    profileAuditAdapter.fireDeleteAction(USER_ID, USER_EMAIL);
    ArgumentCaptor<ActionAuditEvent> captor = ArgumentCaptor.forClass(ActionAuditEvent.class);
    verify(mockActionAuditService).send(captor.capture());
    ActionAuditEvent eventSent = captor.getValue();

    assertEquals(TargetType.PROFILE, eventSent.targetType());
    assertEquals(AgentType.USER, eventSent.agentType());
    assertEquals(USER_ID, eventSent.agentIdMaybe());
    assertEquals(USER_ID, eventSent.targetIdMaybe());
    assertEquals(ActionType.DELETE, eventSent.actionType());
    assertEquals(Y2K_EPOCH_MILLIS, eventSent.timestamp());
    assertNull(eventSent.targetPropertyMaybe());
    assertNull(eventSent.newValueMaybe());
    assertNull(eventSent.previousValueMaybe());
  }
}
