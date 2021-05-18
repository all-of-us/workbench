package org.pmiops.workbench.api;

import static org.pmiops.workbench.billing.GoogleApisConfig.END_USER_CLOUD_BILLING;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.ListBillingAccountsResponse;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.BillingAccount;
import org.pmiops.workbench.model.UserResponse;
import org.pmiops.workbench.model.WorkbenchListBillingAccountsResponse;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApiDelegate {

  private static final Logger log = Logger.getLogger(UserController.class.getName());
  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final String DEFAULT_SORT_FIELD = "username";
  private static final Function<DbUser, org.pmiops.workbench.model.User> TO_USER_RESPONSE_USER =
      user -> {
        org.pmiops.workbench.model.User modelUser = new org.pmiops.workbench.model.User();
        modelUser.setEmail(user.getUsername()); // deprecated, but kept for compatibility
        modelUser.setUserName(user.getUsername());
        modelUser.setGivenName(user.getGivenName());
        modelUser.setFamilyName(user.getFamilyName());
        return modelUser;
      };

  private final Provider<Cloudbilling> cloudBillingProvider;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> configProvider;

  private final AccessTierService accessTierService;
  private final FireCloudService fireCloudService;
  private final FreeTierBillingService freeTierBillingService;
  private final UserService userService;

  @Autowired
  public UserController(
      @Qualifier(END_USER_CLOUD_BILLING) Provider<Cloudbilling> cloudBillingProvider,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> configProvider,
      AccessTierService accessTierService,
      FireCloudService fireCloudService,
      FreeTierBillingService freeTierBillingService,
      UserService userService) {
    this.cloudBillingProvider = cloudBillingProvider;
    this.userProvider = userProvider;
    this.configProvider = configProvider;
    this.accessTierService = accessTierService;
    this.fireCloudService = fireCloudService;
    this.freeTierBillingService = freeTierBillingService;
    this.userService = userService;
  }

  /**
   * Return a page of users matching a search term. Used by autocomplete for workspace sharing.
   *
   * @param term
   * @param pageToken
   * @param pageSize
   * @param sortOrder
   * @return
   */
  @Override
  public ResponseEntity<UserResponse> user(
      String term, String pageToken, Integer pageSize, String sortOrder) {
    UserResponse response = new UserResponse();
    response.setUsers(Collections.emptyList());
    response.setNextPageToken("");

    if (null == term || term.isEmpty()) {
      return ResponseEntity.ok(response);
    }

    PaginationToken paginationToken;
    try {
      paginationToken = getPaginationTokenFromPageToken(pageToken);
    } catch (IllegalArgumentException | BadRequestException e) {
      return ResponseEntity.badRequest().body(response);
    }

    // See discussion on RW-2894. This may not be strictly necessary, especially if researchers
    // details will be published publicly, but it prevents arbitrary unregistered users from seeing
    // limited researcher profile details.

    // also TODO RW-6190: add access tier parameter to this call and check that tier's
    // membership specifically

    if (!isUserInRegisteredTier()) {
      throw new ForbiddenException("user search requires registered data access");
    }

    Sort.Direction direction =
        Sort.Direction.fromOptionalString(sortOrder).orElse(Sort.Direction.ASC);
    Sort sort = Sort.by(new Sort.Order(direction, DEFAULT_SORT_FIELD));

    // What we are really looking for here are users who have a FC account.
    // This should exist if they have signed in at least once
    List<DbUser> users =
        userService.findUsersBySearchString(term, sort).stream()
            .filter(user -> user.getFirstSignInTime() != null)
            .collect(Collectors.toList());

    List<List<DbUser>> pagedUsers =
        Lists.partition(users, Optional.ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE));

    int pageOffset = Long.valueOf(paginationToken.getOffset()).intValue();

    if (pagedUsers.size() == 0) {
      return ResponseEntity.ok(response);
    }

    if (pageOffset < pagedUsers.size()) {
      boolean hasNext = pageOffset < pagedUsers.size() - 1;
      if (hasNext) {
        response.setNextPageToken(PaginationToken.of(pageOffset + 1).toBase64());
      }
      List<org.pmiops.workbench.model.User> modelUsers =
          pagedUsers.get(pageOffset).stream()
              .map(TO_USER_RESPONSE_USER)
              .collect(Collectors.toList());
      response.setUsers(modelUsers);
    } else {
      log.warning(
          String.format(
              "User attempted autocomplete for a paged result that doesn't exist. Term: %s. Page: %d",
              term, pageOffset));
      return ResponseEntity.badRequest().body(response);
    }
    return ResponseEntity.ok(response);
  }

  // check that a user has registered tier membership according to both our DB and Terra/Firecloud
  private boolean isUserInRegisteredTier() {
    return accessTierService.getAccessTiersForUser(userProvider.get()).stream()
        .anyMatch(
            tier ->
                tier.getShortName().equals(AccessTierService.REGISTERED_TIER_SHORT_NAME)
                    && fireCloudService.isUserMemberOfGroup(
                        userProvider.get().getUsername(), tier.getAuthDomainName()));
  }

  @Override
  public ResponseEntity<WorkbenchListBillingAccountsResponse> listBillingAccounts() {
    List<BillingAccount> billingAccounts =
        Stream.concat(maybeFreeTierBillingAccount(), maybeCloudBillingAccounts())
            .collect(Collectors.toList());

    return ResponseEntity.ok(
        new WorkbenchListBillingAccountsResponse().billingAccounts(billingAccounts));
  }

  /**
   * The logic here is a little weird. See RW-4857.
   *
   * <p>When Billing Upgrade is true: return the Free Tier account only when the user has remaining
   * Free Credits.
   *
   * <p>When Billing Upgrade is false: the user's only option is the Free Tier account, so return it
   * even if the user has expired their Free Credits. The UI will disable selection of this option
   * if expired.
   *
   * @return the free tier billing account, if the user has free credits OR billing upgrade is
   *     disabled
   */
  private Stream<BillingAccount> maybeFreeTierBillingAccount() {
    if (configProvider.get().featureFlags.enableBillingUpgrade
        && !freeTierBillingService.userHasRemainingFreeTierCredits(userProvider.get())) {
      return Stream.empty();
    }

    return Stream.of(
        new BillingAccount()
            .isFreeTier(true)
            .displayName("Use All of Us free credits")
            .name(configProvider.get().billing.freeTierBillingAccountName())
            .isOpen(true));
  }

  private Stream<BillingAccount> maybeCloudBillingAccounts() {
    if (!configProvider.get().featureFlags.enableBillingUpgrade) {
      return Stream.empty();
    }

    ListBillingAccountsResponse response;
    try {
      response = cloudBillingProvider.get().billingAccounts().list().execute();
    } catch (IOException e) {
      throw new ServerErrorException("Could not retrieve billing accounts list from Google Cloud");
    }

    return Optional.ofNullable(response.getBillingAccounts()).orElse(Collections.emptyList())
        .stream()
        .map(
            googleBillingAccount ->
                new BillingAccount()
                    .isFreeTier(false)
                    .displayName(googleBillingAccount.getDisplayName())
                    .name(googleBillingAccount.getName())
                    .isOpen(Optional.ofNullable(googleBillingAccount.getOpen()).orElse(false)));
  }

  private PaginationToken getPaginationTokenFromPageToken(String pageToken) {
    return (null == pageToken) ? PaginationToken.of(0) : PaginationToken.fromBase64(pageToken);
  }
}
