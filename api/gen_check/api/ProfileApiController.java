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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import javax.validation.constraints.*;
import javax.validation.Valid;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

@Controller
public class ProfileApiController implements ProfileApi {
    private final ProfileApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public ProfileApiController(ProfileApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<EmptyResponse> bypassAccessRequirement(@ApiParam(value = "",required=true ) @PathVariable("userId") Long userId,
        @ApiParam(value = "Whether the requirement should be bypassed or not. Defaults to true, so an empty body  will cause the requirement to be bypassed. "  )  @Valid @RequestBody AccessBypassRequest bypassed) {
        // do some magic!
        return delegate.bypassAccessRequirement(userId, bypassed);
    }

    public ResponseEntity<Profile> createAccount(@ApiParam(value = ""  )  @Valid @RequestBody CreateAccountRequest createAccountRequest) {
        // do some magic!
        return delegate.createAccount(createAccountRequest);
    }

    public ResponseEntity<Void> deleteProfile() {
        // do some magic!
        return delegate.deleteProfile();
    }

    public ResponseEntity<UserListResponse> getAllUsers() {
        // do some magic!
        return delegate.getAllUsers();
    }

    public ResponseEntity<List<BillingProjectMembership>> getBillingProjects() {
        // do some magic!
        return delegate.getBillingProjects();
    }

    public ResponseEntity<Profile> getMe() {
        // do some magic!
        return delegate.getMe();
    }

    public ResponseEntity<Profile> getUser(@ApiParam(value = "",required=true ) @PathVariable("userId") Long userId) {
        // do some magic!
        return delegate.getUser(userId);
    }

    public ResponseEntity<Void> invitationKeyVerification(@ApiParam(value = ""  )  @Valid @RequestBody InvitationVerificationRequest invitationVerificationRequest) {
        // do some magic!
        return delegate.invitationKeyVerification(invitationVerificationRequest);
    }

    public ResponseEntity<UsernameTakenResponse> isUsernameTaken( @NotNull@ApiParam(value = "", required = true) @RequestParam(value = "username", required = true) String username) {
        // do some magic!
        return delegate.isUsernameTaken(username);
    }

    public ResponseEntity<Profile> requestBetaAccess() {
        // do some magic!
        return delegate.requestBetaAccess();
    }

    public ResponseEntity<Void> resendWelcomeEmail(@ApiParam(value = ""  )  @Valid @RequestBody ResendWelcomeEmailRequest resendWelcomeEmail) {
        // do some magic!
        return delegate.resendWelcomeEmail(resendWelcomeEmail);
    }

    public ResponseEntity<Profile> submitDataUseAgreement( @NotNull@ApiParam(value = "Version \\# of the Data Use Agreement that was signed.", required = true) @RequestParam(value = "dataUseAgreementSignedVersion", required = true) Integer dataUseAgreementSignedVersion,
         @NotNull@ApiParam(value = "Initials of the user on the form.", required = true) @RequestParam(value = "initials", required = true) String initials) {
        // do some magic!
        return delegate.submitDataUseAgreement(dataUseAgreementSignedVersion, initials);
    }

    public ResponseEntity<Profile> submitDemographicsSurvey() {
        // do some magic!
        return delegate.submitDemographicsSurvey();
    }

    public ResponseEntity<Profile> syncComplianceTrainingStatus() {
        // do some magic!
        return delegate.syncComplianceTrainingStatus();
    }

    public ResponseEntity<Profile> syncEraCommonsStatus() {
        // do some magic!
        return delegate.syncEraCommonsStatus();
    }

    public ResponseEntity<Profile> syncTwoFactorAuthStatus() {
        // do some magic!
        return delegate.syncTwoFactorAuthStatus();
    }

    public ResponseEntity<EmptyResponse> unsafeSelfBypassAccessRequirement(@ApiParam(value = "Whether the requirement should be bypassed or not. Defaults to true, so an empty body  will cause the requirement to be bypassed. "  )  @Valid @RequestBody AccessBypassRequest bypassed) {
        // do some magic!
        return delegate.unsafeSelfBypassAccessRequirement(bypassed);
    }

    public ResponseEntity<Void> updateContactEmail(@ApiParam(value = ""  )  @Valid @RequestBody UpdateContactEmailRequest updateContactEmailRequest) {
        // do some magic!
        return delegate.updateContactEmail(updateContactEmailRequest);
    }

    public ResponseEntity<Profile> updateNihToken(@ApiParam(value = "the token retrieved from NIH"  )  @Valid @RequestBody NihToken token) {
        // do some magic!
        return delegate.updateNihToken(token);
    }

    public ResponseEntity<Profile> updatePageVisits(@ApiParam(value = "the users pageVisits"  )  @Valid @RequestBody PageVisit pageVisit) {
        // do some magic!
        return delegate.updatePageVisits(pageVisit);
    }

    public ResponseEntity<Void> updateProfile(@ApiParam(value = "the new profile to use"  )  @Valid @RequestBody Profile updatedProfile) {
        // do some magic!
        return delegate.updateProfile(updatedProfile);
    }

}
