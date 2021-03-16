package org.pmiops.workbench.ras;

import static org.pmiops.workbench.ras.OAuthHelper.decodedJwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;
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
 * Extracts user's login.gov account from UserInfo response in Json format.
 *
 * <p>A valid login.gov userInfo response should contain <i>preferred_username</i> field which
 * ends with <i>@Login.Gov</i> and <i>userid</i> field. Example:
 * <pre>{@code
 *  "sub":"123456abc",
 *  "name" : " ",
 *  "preferred_username":"user1@Login.Gov",
 *  "userid":"user1",
 *  "email" : "foo@gmail.com"
 * }</pre>
 */
@Service
public class RasLinkService {
  private static final Logger log = Logger.getLogger(RasLinkService.class.getName());

  //The RAS url suffix for exchanging token using auth code.
  private static final String TOKEN_URL_SUFFIX = "/auth/oauth/v2/token";
  // The RAS url suffix for initializing authorize request.
  private static final String AUTHORIZE_URL_SUFFIX = "/auth/oauth/v2/authorize";
  // The RAS url suffix for fetching user info, i.e. GA4GH Passport.
  private static final String USER_INFO_URL_SUFFIX = "/openid/connect/v1.1/userinfo";
  // The GCP bucket name that stores RAS secret
  private static final String RAS_SECRET_BUCKET_NAME = "ras-client-secret.txt";
  // The scopes for RAS's auth code exchange. Use ga4gh_passport_v1 to be able to get GA4GH passport
  // using the access token.


  private static final Set<String> RAS_AUTH_CODE_SCOPES = ImmutableSet.of("ga4gh_passport_v1");

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

  public Profile linkRasLoginGovAccount(String authCode, String redirectUrl) {
    String rasClientSecret = cloudStorageClient.getCredentialsBucketString(RAS_SECRET_BUCKET_NAME);
    String rasClientId = workbenchConfigProvider.get().ras.clientId;
    String rasTokenUrl = workbenchConfigProvider.get().ras.host + TOKEN_URL_SUFFIX;
    String rasAuthorizeUrl = workbenchConfigProvider.get().ras.host + AUTHORIZE_URL_SUFFIX;

    try {
    AuthorizationCodeFlow flow = OAuthHelper.newAuthCodeFlow(rasClientId, rasClientSecret, rasTokenUrl, rasAuthorizeUrl);
    TokenResponse tokenResponse = OAuthHelper.codeExchange(flow, authCode, redirectUrl, RAS_AUTH_CODE_SCOPES);
    String acrClaim = decodedJwt(tokenResponse.get("id_token").toString()).getClaim("acr").asString();
    if(!isIal2(acrClaim)) {
      throw new ForbiddenException(String.format("User does not have IAL2 enabled, acrClaim: %s", acrClaim));
    }
    JsonNode userInfoResponse = objectMapper.readTree(OAuthHelper.fetchUserInfo(tokenResponse.getAccessToken(), workbenchConfigProvider.get().ras.host + USER_INFO_URL_SUFFIX));
    userService.updateRasLinkLoginGovStatus(getLoginGovUserId(userInfoResponse));
    } catch (IOException e) {
      log.log(Level.WARNING, "Failed to link RAS account", e);
    }
    return profileService.getProfile(userProvider.get());
  }

  /***/
  private static boolean isIal2(String acrClaim) {
    Pattern p = Pattern.compile("ial/\\d");
    Matcher m = p.matcher("aclClaim");
    if(m.matches()) {
      return m.group().equals("2");
    }
    throw new ServerErrorException(String.format("Invalid acl Claim in OIDC id token for %s", acrClaim));
  }

  /**
   * Extracts user's login.gov account from UserInfo response in Json format.
   *
   * <p>A valid login.gov userInfo response should contain <i>preferred_username</i> field which
   * ends with <i>@Login.Gov</i> and we can use that as user's unique identifier. Example:
   * <pre>{@code
   *  "sub":"123456abc",
   *  "name" : " ",
   *  "preferred_username":"user1@Login.Gov",
   *  "email" : "foo@gmail.com"
   * }</pre>
   */
  private static String getLoginGovUserId(JsonNode userInfo) {
    String preferredUsername = userInfo.get("preferred_username").asText("");
    if(!preferredUsername.toLowerCase().contains("@login.gov")) {
      throw new ForbiddenException(String.format("User does not have valid login.gov account, invalid preferred_username: %s", preferredUsername));
    }
    return preferredUsername;
  }
}
