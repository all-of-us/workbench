package org.pmiops.workbench.api;

import static org.pmiops.workbench.google.GoogleConfig.END_USER_CLOUD_BILLING;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.ListBillingAccountsResponse;
import jakarta.inject.Provider;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.BillingAccount;
import org.pmiops.workbench.model.WorkbenchListBillingAccountsResponse;
import org.pmiops.workbench.user.VwbUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApiDelegate {
  private final Provider<Cloudbilling> cloudBillingProvider;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> configProvider;
  private final InitialCreditsService initialCreditsService;
  private final UserService userService;
  private final VwbUserService vwbUserService;

  @Autowired
  public UserController(
      @Qualifier(END_USER_CLOUD_BILLING) Provider<Cloudbilling> cloudBillingProvider,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> configProvider,
      InitialCreditsService initialCreditsService,
      UserService userService,
      VwbUserService vwbUserService) {
    this.cloudBillingProvider = cloudBillingProvider;
    this.userProvider = userProvider;
    this.configProvider = configProvider;
    this.initialCreditsService = initialCreditsService;
    this.userService = userService;
    this.vwbUserService = vwbUserService;
  }

  @Override
  public ResponseEntity<WorkbenchListBillingAccountsResponse> listBillingAccounts() {
    List<BillingAccount> billingAccounts =
        Stream.concat(maybeInitialCreditsAccount(), maybeCloudBillingAccounts()).toList();

    return ResponseEntity.ok(
        new WorkbenchListBillingAccountsResponse().billingAccounts(billingAccounts));
  }

  @Override
  public ResponseEntity<Void> signOut() {
    userService.signOut(userProvider.get());
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Boolean> getUserTosStatus() {
    return ResponseEntity.ok(vwbUserService.getUserTosState());
  }

  /**
   * @return the initial credits billing account, if the user has any remaining credits
   */
  private Stream<BillingAccount> maybeInitialCreditsAccount() {
    if (!initialCreditsService.userHasRemainingInitialCredits(userProvider.get())) {
      return Stream.empty();
    }

    return Stream.of(
        new BillingAccount()
            .freeTier(true)
            .displayName("Use All of Us initial credits")
            .name(configProvider.get().billing.initialCreditsBillingAccountName())
            .open(true));
  }

  private Stream<BillingAccount> maybeCloudBillingAccounts() {
    ListBillingAccountsResponse response;
    try {
      response = cloudBillingProvider.get().billingAccounts().list().execute();
    } catch (IOException e) {
      throw new ServerErrorException(
          "Could not retrieve billing accounts list from Google Cloud", e);
    }

    return Optional.ofNullable(response.getBillingAccounts())
        .orElse(Collections.emptyList())
        .stream()
        .map(
            googleBillingAccount ->
                new BillingAccount()
                    .freeTier(false)
                    .displayName(googleBillingAccount.getDisplayName())
                    .name(googleBillingAccount.getName())
                    .open(Boolean.TRUE.equals(googleBillingAccount.getOpen())));
  }
}
