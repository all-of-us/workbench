package org.pmiops.workbench.ras;

import static org.pmiops.workbench.ras.OAuthHelper.decodedJwt;
import static org.pmiops.workbench.ras.RasLinkConstants.ACR_CLAIM;
import static org.pmiops.workbench.ras.RasLinkConstants.AUTHORIZE_URL_SUFFIX;
import static org.pmiops.workbench.ras.RasLinkConstants.Id_TOKEN_FIELD_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.LOGIN_GOV_IDENTIFIER_LOWER_CASE;
import static org.pmiops.workbench.ras.RasLinkConstants.RAS_AUTH_CODE_SCOPES;
import static org.pmiops.workbench.ras.RasLinkConstants.RAS_SECRET_BUCKET_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.TOKEN_URL_SUFFIX;
import static org.pmiops.workbench.ras.RasLinkConstants.USERNAME_FIELD_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.USER_INFO_URL_SUFFIX;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.TokenResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.profile.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service handles link login.gov account with All of Us account. It finishes OAuth dance with RAS
 * then validate users use their login.gov account with IAL2 enabled.
 *
 * <p>Key steps:
 * Step1: Finish OAuth and get {@link TokenResponse}. A sample response:
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
 * The acr claim contains user IAL status, the number after /assurance/ial/
 *
 * Step3: User access_token to pull RAS user info. A sample userInfo in Json format:
 * <pre>{@code
 *  "sub":"subId",
 *  "name" : "",
 *  "preferred_username":"user1@Login.Gov",
 *  "userId":"user1"
 *  "email":"foo@gmail.com"
 * }</pre>
 * The {@code preferred_username} is what we use should use to extract login.goc username. If that
 * field is missing or does not have Login.Gov suffix, that means user not using login.gov to login.
 * In this case, returns {@link ForbiddenException}.
 *
 * Step4: Use step3's login.gov username to update AoU database by
 * {@link UserService#updateRasLinkLoginGovStatus(String)}. Then return it as user profile.
 */
@Service
public class RasLinkService {
  private static final Logger log = Logger.getLogger(RasLinkService.class.getName());

  private final CloudStorageClient cloudStorageClient;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final ObjectMapper objectMapper;
  private final ProfileService profileService;
  private final UserService userService;
  private final Provider<DbUser> userProvider;

  @Autowired
  public RasLinkService(
      CloudStorageClient cloudStorageClient,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      ProfileService profileService, UserService userService,
      Provider<DbUser> userProvider) {
    this.cloudStorageClient = cloudStorageClient;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.profileService = profileService;
    this.userService = userService;
    this.userProvider = userProvider;
    this.objectMapper = new ObjectMapper();
  }

  /** Links RAS login.gov account with AoU account. */
  public Profile linkRasLoginGovAccount(String authCode, String redirectUrl) {
    String rasClientSecret = cloudStorageClient.getCredentialsBucketString(RAS_SECRET_BUCKET_NAME);
    String rasClientId = workbenchConfigProvider.get().ras.clientId;
    String rasTokenUrl = workbenchConfigProvider.get().ras.host + TOKEN_URL_SUFFIX;
    String rasAuthorizeUrl = workbenchConfigProvider.get().ras.host + AUTHORIZE_URL_SUFFIX;
    JsonNode userInfoResponse = NullNode.getInstance();
    try {
      // Oauth dance to get id token and access token.
    AuthorizationCodeFlow flow = OAuthHelper.newAuthCodeFlow(rasClientId, rasClientSecret, rasTokenUrl, rasAuthorizeUrl);
    TokenResponse tokenResponse = OAuthHelper.codeExchange(flow, authCode, redirectUrl, RAS_AUTH_CODE_SCOPES);
    // Validate IAL status.
    String acrClaim = decodedJwt(tokenResponse.get(Id_TOKEN_FIELD_NAME).toString()).getClaim(ACR_CLAIM).asString();
    if(!isIal2(acrClaim)) {
      throw new ForbiddenException(String.format("User does not have IAL2 enabled, acrClaim: %s", acrClaim));
    }
    // Fetch user info.
    userInfoResponse = objectMapper.readTree(OAuthHelper.fetchUserInfo(tokenResponse.getAccessToken(), workbenchConfigProvider.get().ras.host + USER_INFO_URL_SUFFIX));
    } catch (IOException e) {
      log.log(Level.WARNING, "Failed to link RAS account", e);
    }
    DbUser dbUser = userService.updateRasLinkLoginGovStatus(getLoginGovUserId(userInfoResponse));
    return profileService.getProfile(dbUser);
  }

  /** Validates user has IAL2 setup. See class javadoc Step2 for more details. */
  private static boolean isIal2(String acrClaim) {
    Pattern p = Pattern.compile("ial/\\d", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(acrClaim);
    if(m.matches()) {
      return m.group().equals("2");
    }
    throw new ServerErrorException(String.format("Invalid acl Claim in OIDC id token for %s", acrClaim));
  }

  /**
   * Extracts user's login.gov account from UserInfo response in Json format. See class javadoc
   * Step3 for more details.
   */
  private static String getLoginGovUserId(JsonNode userInfo) {
    String preferredUsername = userInfo.get(USERNAME_FIELD_NAME).asText("");
    if(!preferredUsername.toLowerCase().contains(LOGIN_GOV_IDENTIFIER_LOWER_CASE)) {
      throw new ForbiddenException(String.format("User does not have valid login.gov account, invalid preferred_username: %s", preferredUsername));
    }
    return preferredUsername;
  }
}
