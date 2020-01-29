package org.pmiops.workbench.db.dao;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.Function;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Degree;
import org.springframework.data.domain.Sort;

public interface UserService {
  DbUser updateUserWithRetries(Function<DbUser, DbUser> userModifier, DbUser dbUser);

  DbUser createServiceAccountUser(String email);

  DbUser createUser(
      String givenName,
      String familyName,
      String email,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch);

  DbUser createUser(
      String givenName,
      String familyName,
      String email,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch,
      String professionalUrl,
      List<Degree> degrees,
      DbAddress address,
      DbDemographicSurvey demographicSurvey,
      List<DbInstitutionalAffiliation> institutionalAffiliations);

  DbUser submitDataUseAgreement(
      DbUser user, Integer dataUseAgreementSignedVersion, String initials);

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
  DbUser syncComplianceTrainingStatusV1(DbUser user)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncComplianceTrainingStatusV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncComplianceTrainingStatusV2(DbUser user)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncEraCommonsStatus();

  DbUser syncEraCommonsStatusUsingImpersonation(DbUser user)
      throws IOException, org.pmiops.workbench.firecloud.ApiException;

  void syncTwoFactorAuthStatus();

  DbUser syncTwoFactorAuthStatus(DbUser targetUser);
}
