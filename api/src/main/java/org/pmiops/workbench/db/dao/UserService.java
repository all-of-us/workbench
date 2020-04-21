package org.pmiops.workbench.db.dao;

import com.google.api.services.oauth2.model.Userinfoplus;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.Function;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Degree;
import org.springframework.data.domain.Sort;

public interface UserService {
  DbUser updateUserWithRetries(Function<DbUser, DbUser> userModifier, DbUser dbUser, Agent agent);

  DbUser createServiceAccountUser(String email);

  // version used by AuthInterceptor
  DbUser createUser(Userinfoplus oAuth2Userinfo);

  DbUser createUser(
      String givenName,
      String familyName,
      String userName,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch,
      String professionalUrl,
      List<Degree> degrees,
      DbAddress dbAddress,
      DbDemographicSurvey dbDemographicSurvey,
      List<DbInstitutionalAffiliation> dbAffiliations,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation);

  DbUser updateUserWithConflictHandling(DbUser user, DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation);

  DbUser submitDataUseAgreement(
      DbUser user, Integer dataUseAgreementSignedVersion, String initials);

  // Registers that a user has agreed to a given version of the Terms of Service.
  void submitTermsOfService(DbUser dbUser, Integer tosVersion);

  void setDataUseAgreementNameOutOfDate(String newGivenName, String newFamilyName);

  void setDataUseAgreementBypassTime(Long userId, Timestamp bypassTime);

  void setComplianceTrainingBypassTime(Long userId, Timestamp bypassTime);

  void setBetaAccessBypassTime(Long userId, Timestamp bypassTime);

  void setEraCommonsBypassTime(Long userId, Timestamp bypassTime);

  void setTwoFactorAuthBypassTime(Long userId, Timestamp bypassTime);

  void setClusterRetryCount(int clusterRetryCount);

  DbUser setDisabledStatus(Long userId, boolean disabled);

  List<DbUser> getAllUsers();

  void logAdminUserAction(long targetUserId, String targetAction, Object oldValue, Object newValue);

  void logAdminWorkspaceAction(
      long targetWorkspaceId, String targetAction, Object oldValue, Object newValue);

  List<DbUser> findUsersBySearchString(String term, Sort sort);

  @Deprecated
  DbUser syncComplianceTrainingStatusV1()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  @Deprecated
  DbUser syncComplianceTrainingStatusV1(DbUser user, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncComplianceTrainingStatusV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncComplianceTrainingStatusV2(DbUser user, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncEraCommonsStatus();

  DbUser syncEraCommonsStatusUsingImpersonation(DbUser user, Agent agent)
      throws IOException, org.pmiops.workbench.firecloud.ApiException;

  void syncTwoFactorAuthStatus();

  DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent);

  int getCurrentDuccVersion();
}
