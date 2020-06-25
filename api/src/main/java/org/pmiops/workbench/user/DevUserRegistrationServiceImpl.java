package org.pmiops.workbench.user;

import com.google.api.services.oauth2.model.Userinfoplus;
import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DevUserRegistrationServiceImpl implements DevUserRegistrationService {

  private static final Logger log =
      Logger.getLogger(DevUserRegistrationServiceImpl.class.getName());

  private final DirectoryService directoryService;
  private final InstitutionService institutionService;
  private final UserService userService;
  private final VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper;

  @Autowired
  DevUserRegistrationServiceImpl(
      DirectoryService directoryService,
      InstitutionService institutionService,
      UserService userService,
      VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper) {
    this.directoryService = directoryService;
    this.institutionService = institutionService;
    this.userService = userService;
    this.verifiedInstitutionalAffiliationMapper = verifiedInstitutionalAffiliationMapper;
  }

  @Override
  public DbUser createUser(Userinfoplus userInfo) {
    // We'll try to lookup the GSuite contact email if available. Otherwise, fall back to the
    // username email address (e.g. foobar@fake-research-aou.org).
    Optional<String> gSuiteContactEmail =
        directoryService.getContactEmailFromGSuiteEmail(userInfo.getEmail());
    String contactEmail = gSuiteContactEmail.orElse(userInfo.getEmail());

    log.info(
        String.format(
            "Re-creating dev user '%s' with contact email '%s'.",
            userInfo.getEmail(), contactEmail));

    Institution institution =
        institutionService
            .getFirstMatchingInstitution(contactEmail)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        String.format(
                            "Contact email %s does not match any institutions. Cannot register new dev user.",
                            contactEmail)));
    VerifiedInstitutionalAffiliation verifiedAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(institution.getShortName())
            .institutionalRoleEnum(InstitutionalRole.OTHER)
            .institutionalRoleOtherText("System developer");

    return userService.createUser(
        userInfo,
        contactEmail,
        verifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            verifiedAffiliation, institutionService));
  }
}
