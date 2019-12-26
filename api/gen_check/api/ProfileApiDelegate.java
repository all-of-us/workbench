package org.pmiops.workbench.api;

import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.BillingProjectMembership;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.InvitationVerificationRequest;
import org.pmiops.workbench.model.NihToken;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.model.UserListResponse;
import org.pmiops.workbench.model.UsernameTakenResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link ProfileApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link ProfileApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public interface ProfileApiDelegate {

    /**
     * @see ProfileApi#bypassAccessRequirement
     */
    ResponseEntity<EmptyResponse> bypassAccessRequirement(Long userId,
        AccessBypassRequest bypassed);

    /**
     * @see ProfileApi#createAccount
     */
    ResponseEntity<Profile> createAccount(CreateAccountRequest createAccountRequest);

    /**
     * @see ProfileApi#deleteProfile
     */
    ResponseEntity<Void> deleteProfile();

    /**
     * @see ProfileApi#getAllUsers
     */
    ResponseEntity<UserListResponse> getAllUsers();

    /**
     * @see ProfileApi#getBillingProjects
     */
    ResponseEntity<List<BillingProjectMembership>> getBillingProjects();

    /**
     * @see ProfileApi#getMe
     */
    ResponseEntity<Profile> getMe();

    /**
     * @see ProfileApi#getUser
     */
    ResponseEntity<Profile> getUser(Long userId);

    /**
     * @see ProfileApi#invitationKeyVerification
     */
    ResponseEntity<Void> invitationKeyVerification(InvitationVerificationRequest invitationVerificationRequest);

    /**
     * @see ProfileApi#isUsernameTaken
     */
    ResponseEntity<UsernameTakenResponse> isUsernameTaken(String username);

    /**
     * @see ProfileApi#requestBetaAccess
     */
    ResponseEntity<Profile> requestBetaAccess();

    /**
     * @see ProfileApi#resendWelcomeEmail
     */
    ResponseEntity<Void> resendWelcomeEmail(ResendWelcomeEmailRequest resendWelcomeEmail);

    /**
     * @see ProfileApi#submitDataUseAgreement
     */
    ResponseEntity<Profile> submitDataUseAgreement(Integer dataUseAgreementSignedVersion,
        String initials);

    /**
     * @see ProfileApi#submitDemographicsSurvey
     */
    ResponseEntity<Profile> submitDemographicsSurvey();

    /**
     * @see ProfileApi#syncComplianceTrainingStatus
     */
    ResponseEntity<Profile> syncComplianceTrainingStatus();

    /**
     * @see ProfileApi#syncEraCommonsStatus
     */
    ResponseEntity<Profile> syncEraCommonsStatus();

    /**
     * @see ProfileApi#syncTwoFactorAuthStatus
     */
    ResponseEntity<Profile> syncTwoFactorAuthStatus();

    /**
     * @see ProfileApi#unsafeSelfBypassAccessRequirement
     */
    ResponseEntity<EmptyResponse> unsafeSelfBypassAccessRequirement(AccessBypassRequest bypassed);

    /**
     * @see ProfileApi#updateContactEmail
     */
    ResponseEntity<Void> updateContactEmail(UpdateContactEmailRequest updateContactEmailRequest);

    /**
     * @see ProfileApi#updateNihToken
     */
    ResponseEntity<Profile> updateNihToken(NihToken token);

    /**
     * @see ProfileApi#updatePageVisits
     */
    ResponseEntity<Profile> updatePageVisits(PageVisit pageVisit);

    /**
     * @see ProfileApi#updateProfile
     */
    ResponseEntity<Void> updateProfile(Profile updatedProfile);

}
