package org.pmiops.workbench.ras;

import static org.pmiops.workbench.ras.OpenIdConnectClient.decodedJwt;
import static org.pmiops.workbench.ras.RasLinkConstants.ACR_CLAIM;
import static org.pmiops.workbench.ras.RasLinkConstants.ACR_CLAIM_IAL_2_IDENTIFIER;
import static org.pmiops.workbench.ras.RasLinkConstants.ERA_COMMONS_PROVIDER_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.FEDERATED_IDENTITIES;
import static org.pmiops.workbench.ras.RasLinkConstants.IDENTITIES;
import static org.pmiops.workbench.ras.RasLinkConstants.IDENTITY_USERID;
import static org.pmiops.workbench.ras.RasLinkConstants.Id_TOKEN_FIELD_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.LOGIN_GOV_IDENTIFIER_LOWER_CASE;
import static org.pmiops.workbench.ras.RasLinkConstants.PREFERRED_USERNAME_FIELD_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.RAS_AUTH_CODE_SCOPES;
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
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Service handles link login.gov account with All of Us account. It finishes OAuth dance with RAS
 * then validate users use their login.gov account with IAL2 enabled.
 *
 * <p>Key steps: Step1: Finish OAuth and get {@link TokenResponse}. A sample response:
 *
 * <pre>{@code
 * "access_token":"JWT token"
 * "token_type":"Bearer",
 * "expires_in":1800,
 * "refresh_token":"refresh token",
 * "scope":"openid profile email ga4gh_passport_v1",
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
 *    "identities":[
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
 *  ]
 *  }
 * }</pre>
 *
 * The {@code preferred_username} field should end with "@login.gov" if using that to login. The
 * {@code preferred_username} field is unique login.gov username. We can use that as login.gov user
 * name.
 *
 * <p>Step4: Use step3's login.gov username to update AoU database by {@link
 * UserService#updateRasLinkLoginGovStatus(String)}. Then return it as user profile.
 *
 * <p>TODO(yonghao): Fow now we return {@llink ForbiddenException} for all scenarios, determine if
 * we need to differentiate IAL vs Login.gov scenarios, and give that information to UI.
 */
@Service
public class RasLinkService {
  private static final Logger log = Logger.getLogger(RasLinkService.class.getName());

  private final AccessModuleService accessModuleService;
  private final UserService userService;
  private final Provider<OpenIdConnectClient> rasOidcClientProvider;

  @Autowired
  public RasLinkService(
      AccessModuleService accessModuleService,
      UserService userService,
      @Qualifier(RAS_OIDC_CLIENT) Provider<OpenIdConnectClient> rasOidcClientProvider) {
    this.accessModuleService = accessModuleService;
    this.userService = userService;
    this.rasOidcClientProvider = rasOidcClientProvider;
  }

  /** Links RAS login.gov account with AoU account. */
  public DbUser linkRasLoginGovAccount(String authCode, String redirectUrl) {
    OpenIdConnectClient rasOidcClient = rasOidcClientProvider.get();
    JsonNode userInfoResponse;
    try {
      // Oauth dance to get id token and access token.
      TokenResponse tokenResponse =
          rasOidcClient.codeExchange(authCode, decodeUrl(redirectUrl), RAS_AUTH_CODE_SCOPES);

      // Validate IAL status.
      String acrClaim =
          decodedJwt(tokenResponse.get(Id_TOKEN_FIELD_NAME).toString())
              .getClaim(ACR_CLAIM)
              .asString();

      if (!isIal2(acrClaim)) {
        log.warning(String.format("User does not have IAL2 enabled, acrClaim: %s", acrClaim));
        throw new ForbiddenException(
            String.format("User does not have IAL2 enabled, acrClaim: %s", acrClaim));
      }
      // Fetch user info.
      userInfoResponse = rasOidcClient.fetchUserInfo(tokenResponse.getAccessToken());
    } catch (IOException e) {
      log.log(Level.WARNING, "Failed to link RAS account", e);
      throw new ServerErrorException("Failed to link RAS account", e);
    }

    // If eRA is not already linked, check response from RAS see if RAS contains eRA Linking
    // information.
    DbUser user = userService.updateRasLinkLoginGovStatus(getLoginGovUsername(userInfoResponse));
    Optional<AccessModuleStatus> eRAModuleStatus =
        accessModuleService.getClientAccessModuleStatus(user).stream()
            .filter(a -> a.getModuleName() == AccessModule.ERA_COMMONS)
            .findFirst();
    if (eRAModuleStatus.get().getCompletionEpochMillis() != null
        || eRAModuleStatus.get().getBypassEpochMillis() != null) {
      return user;
    }
    Optional<String> eRaUserId = getEraUserId(userInfoResponse);
    if (eRaUserId.isPresent() && !eRaUserId.get().isEmpty()) {
      return userService.updateRasLinkEraStatus(eRaUserId.get());
    } else {
      log.info(String.format("User does not have valid eRA %s", userInfoResponse));
    }
    return user;
  }

  /** Validates user has IAL2 setup. See class javadoc Step2 for more details. */
  static boolean isIal2(String acrClaim) {
    return acrClaim.contains(ACR_CLAIM_IAL_2_IDENTIFIER);
  }

  /**
   * Validates and extracts user's login.gov account from UserInfo response in Json format. See
   * class javadoc Step3 for more details.
   */
  private static String getLoginGovUsername(JsonNode userInfo) {
    String preferredUsername = userInfo.get(PREFERRED_USERNAME_FIELD_NAME).asText("");
    if (!preferredUsername.toLowerCase().contains(LOGIN_GOV_IDENTIFIER_LOWER_CASE)) {
      throw new ForbiddenException(
          String.format(
              "User does not have valid login.gov account, preferred_username: %s",
              preferredUsername));
    }
    return preferredUsername;
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
        .map(u -> u.get(0))
        .map(u -> u.get(ERA_COMMONS_PROVIDER_NAME))
        .map(u -> u.get(IDENTITY_USERID))
        .map(JsonNode::asText);
  }

  /** Decode an encoded url */
  private static String decodeUrl(String encodedUrl) throws UnsupportedEncodingException {
    return URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString());
  }
}
