package org.pmiops.workbench.ras;

import static org.pmiops.workbench.ras.OpenIdConnectClient.decodedJwt;
import static org.pmiops.workbench.ras.RasLinkConstants.ACCESS_TOKEN_FIELD_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.ACR_CLAIM;
import static org.pmiops.workbench.ras.RasLinkConstants.ACR_CLAIM_IAL_2_IDENTIFIER;
import static org.pmiops.workbench.ras.RasLinkConstants.ERA_COMMONS_PROVIDER_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.FEDERATED_IDENTITIES;
import static org.pmiops.workbench.ras.RasLinkConstants.IDENTITIES;
import static org.pmiops.workbench.ras.RasLinkConstants.IDENTITY_USERID;
import static org.pmiops.workbench.ras.RasLinkConstants.ID_ME_IDENTIFIER_LOWER_CASE;
import static org.pmiops.workbench.ras.RasLinkConstants.Id_TOKEN_FIELD_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.LOGIN_GOV_IDENTIFIER_LOWER_CASE;
import static org.pmiops.workbench.ras.RasLinkConstants.PREFERRED_USERNAME_FIELD_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.RAS_AUTH_CODE_SCOPES;
import static org.pmiops.workbench.ras.RasLinkConstants.TXN_CLAIM;
import static org.pmiops.workbench.ras.RasOidcClientConfig.RAS_OIDC_CLIENT;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.auth.oauth2.TokenResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbIdentityVerification.DbIdentityVerificationSystem;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.identityverification.IdentityVerificationService;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Service handles linking the user's selected identity provider account with All of Us account. It
 * finishes OAuth dance with RAS then validate users use their RAS account with IAL2 enabled.
 *
 * <p>Key steps: Step1: Finish OAuth and get {@link TokenResponse}. A sample response:
 *
 * <pre>{@code
 * "access_token":"JWT token"
 * "token_type":"Bearer",
 * "expires_in":1800,
 * "refresh_token":"refresh token",
 * "scope":"openid profile email",
 * "sub":"subId",
 * "id_token":"JWT token"
 * "id_token_type":"urn:ietf:params:oauth:grant-type:jwt-bearer"
 * }</pre>
 *
 * Step2: Decode JWT id_token to extract IAL status. Decoded id_token:
 *
 * <pre>{@code
 * "sub": "subId",
 * "aud": "client id",
 * "c_hash": "",
 * "acr": "https://stsstg.nih.gov/assurance/ial/1 https://stsstg.nih.gov/assurance/aal/1",
 * "azp": "",
 * "auth_time": 1615910504,
 * "iss": "https://stsstg.nih.gov",
 * "exp": 1615996939,
 * "iat": 1615910539
 * }</pre>
 *
 * The acr claim contains user IAL status, the number after /assurance/ial/
 *
 * <p>Step3: User access_token to pull RAS user info. A sample userInfo in Json format:
 *
 * <pre>{@code
 * "sub":"subId",
 * "name" : "",
 * "preferred_username":"user1@Login.Gov",
 * "userId":"user1"
 * "email":"foo@gmail.com"
 * "federated_identities":{
 *    "identities":
 *    {
 *       "login.gov":{
 *          "firstname":"",
 *          "lastname":"",
 *          "userid":"12345",
 *          "mail":"foo@gmail.com"
 *       },
 *       "era":{
 *          "mail":"bar@email.com",
 *          "firstname":"Y",
 *          "lastname":"yyu",
 *          "userid":"yyu_pi"
 *       }
 *    }
 *  }
 * }</pre>
 *
 * The {@code preferred_username} field should end with either "@login.gov" or "@id.me" if using
 * that to login. The {@code preferred_username} field is unique login.gov|id.me username. We can
 * use that as login.|id.me user name.
 *
 * <p>Step4: Use step3's RAS username to update AoU database by {@link *
 * UserService#updateRasLinkLoginGovStatus(String)} or {@link *
 * UserService#updateIdentityStatus(String)} (based on which service was used). Then return it as *
 * user profile.
 *
 * <p>TODO(yonghao): Fow now we return {@link ForbiddenException} for all scenarios, determine if we
 * need to differentiate IAL vs Login.gov scenarios, and give that information to UI.
 */
@Service
public class RasLinkService {
  private static final Logger log = Logger.getLogger(RasLinkService.class.getName());

  private final AccessModuleService accessModuleService;
  private final UserService userService;

  private final IdentityVerificationService identityVerificationService;
  private final Provider<OpenIdConnectClient> rasOidcClientProvider;

  private final Provider<DbUser> userProvider;

  @Autowired
  public RasLinkService(
      AccessModuleService accessModuleService,
      UserService userService,
      IdentityVerificationService identityVerificationService,
      @Qualifier(RAS_OIDC_CLIENT) Provider<OpenIdConnectClient> rasOidcClientProvider,
      Provider<DbUser> userProvider) {
    this.accessModuleService = accessModuleService;
    this.userService = userService;
    this.identityVerificationService = identityVerificationService;
    this.rasOidcClientProvider = rasOidcClientProvider;
    this.userProvider = userProvider;
  }

  /** Links RAS account with AoU account. */
  public DbUser linkRasAccount(String authCode, String redirectUrl) {
    OpenIdConnectClient rasOidcClient = rasOidcClientProvider.get();
    JsonNode userInfoResponse;
    String txnClaim;
    String aouUsername = userProvider.get().getUsername();
    try {
      // Oauth dance to get id token and access token.
      TokenResponse tokenResponse =
          rasOidcClient.codeExchange(authCode, decodeUrl(redirectUrl), RAS_AUTH_CODE_SCOPES);

      // The txn claim is used by the RAS team in order to associate our logs with theirs.
      txnClaim =
          decodedJwt(tokenResponse.get(ACCESS_TOKEN_FIELD_NAME).toString())
              .getClaim(TXN_CLAIM)
              .asString();

      log.info(
          String.format(
              "User (%s) received a valid RAS access token, txn:  %s", aouUsername, txnClaim));
      // Validate IAL status.
      String acrClaim =
          decodedJwt(tokenResponse.get(Id_TOKEN_FIELD_NAME).toString())
              .getClaim(ACR_CLAIM)
              .asString();

      if (!isIal2(acrClaim)) {
        log.warning(
            String.format(
                "User does not have IAL2 enabled, acrClaim: %s, txn: %s", acrClaim, txnClaim));
        throw new ForbiddenException(
            String.format("User does not have IAL2 enabled, acrClaim: %s", acrClaim));
      }
      // Fetch user info.
      userInfoResponse = rasOidcClient.fetchUserInfo(tokenResponse.getAccessToken());
      log.info(
          String.format(
              "Successfully retrieved OIDC user information "
                  + "from RAS access token for user (%s), txn:  %s",
              aouUsername, txnClaim));
    } catch (IOException e) {
      log.log(Level.WARNING, "Failed to link RAS account", e);
      throw new ServerErrorException("Failed to link RAS account", e);
    }

    String rasUsername = getUsername(userInfoResponse);
    DbUser user;
    if (rasUsername.toLowerCase().contains(ID_ME_IDENTIFIER_LOWER_CASE)) {
      user = userService.updateIdentityStatus(rasUsername);
      identityVerificationService.updateIdentityVerificationSystem(
          user, DbIdentityVerificationSystem.ID_ME);
    } else if (rasUsername.toLowerCase().contains(LOGIN_GOV_IDENTIFIER_LOWER_CASE)) {
      user = userService.updateRasLinkLoginGovStatus(rasUsername);
      identityVerificationService.updateIdentityVerificationSystem(
          user, DbIdentityVerificationSystem.LOGIN_GOV);
    } else {
      log.info(
          String.format(
              "User has neither a valid id.me account "
                  + "nor a valid login.gov account, "
                  + "preferred_username: %s, txn: %s",
              rasUsername, txnClaim));
      throw new ForbiddenException(
          String.format(
              "User has neither a valid id.me account nor a valid login.gov account, preferred_username: %s",
              rasUsername));
    }

    log.info(
        String.format(
            "User (%s) successfully verified their identity with the following RAS username: %s, txn: %s",
            aouUsername, rasUsername, txnClaim));

    // If eRA is not already linked, check response from RAS see if RAS contains eRA Linking
    // information.
    Optional<AccessModuleStatus> eRAModuleStatus =
        accessModuleService.getAccessModuleStatus(user).stream()
            .filter(a -> a.getModuleName() == AccessModule.ERA_COMMONS)
            .findFirst();
    if (eRAModuleStatus.isPresent()
        && (eRAModuleStatus.get().getCompletionEpochMillis() != null
            || eRAModuleStatus.get().getBypassEpochMillis() != null)) {
      return user;
    }
    Optional<String> eRaUserId = getEraUserId(userInfoResponse);
    if (eRaUserId.isPresent() && !eRaUserId.get().isEmpty()) {
      return userService.updateRasLinkEraStatus(eRaUserId.get());
    } else {
      log.info(
          String.format(
              "User does not have valid eRA %s", userInfoResponse.get(FEDERATED_IDENTITIES)));
    }
    return user;
  }

  /** Validates user has IAL2 setup. See class javadoc Step2 for more details. */
  static boolean isIal2(String acrClaim) {
    return acrClaim.contains(ACR_CLAIM_IAL_2_IDENTIFIER);
  }

  /**
   * Validates and extracts user's preferred username from UserInfo response in Json format. See
   * class javadoc Step3 for more details.
   */
  private static String getUsername(JsonNode userInfo) {
    return userInfo.get(PREFERRED_USERNAME_FIELD_NAME).asText("");
  }

  /**
   * Extracts user's eRA commons user id account from UserInfo response if has. See class javadoc
   * for an example of eRA identity.
   *
   * <p>Returns empty if eRA is invalid or not linked.
   */
  public static Optional<String> getEraUserId(JsonNode userInfo) {
    return Optional.of(userInfo)
        .map(u -> u.get(FEDERATED_IDENTITIES))
        .map(u -> u.get(IDENTITIES))
        .map(u -> u.get(ERA_COMMONS_PROVIDER_NAME))
        .map(u -> u.get(IDENTITY_USERID))
        .map(JsonNode::asText);
  }

  /** Decode an encoded url */
  private static String decodeUrl(String encodedUrl) throws UnsupportedEncodingException {
    return URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8);
  }
}
