package org.pmiops.workbench.ras;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

/** Constants used in to process RAS linkage. */
public class RasLinkConstants {
  private RasLinkConstants() {}

  // The RAS url suffix for exchanging token using auth code.
  static final String TOKEN_URL_SUFFIX = "/auth/oauth/v2/token";

  // The RAS url suffix for initializing authorize request.
  static final String AUTHORIZE_URL_SUFFIX = "/auth/oauth/v2/authorize";

  // The RAS url suffix for fetching user info, i.e. GA4GH Passport.
  static final String USER_INFO_URL_SUFFIX = "/openid/connect/v1.1/userinfo";

  // The GCP bucket name that stores RAS secret
  static final String RAS_SECRET_BUCKET_NAME = "ras-client-secret.txt";

  // The OIDC acr (Authentication Context Class Reference) claim name in JWT
  static final String ACR_CLAIM = "acr";

  // The Id Token field name from RAS's TokenResponse
  static final String Id_TOKEN_FIELD_NAME = "id_token";
  // The login.gov's PREFERRED_USERNAME field from RAS UserInfo endpoint
  static final String PREFERRED_USERNAME_FIELD_NAME = "preferred_username";

  // The login.gov's FEDERATED_IDENTITIES field from RAS UserInfo endpoint
  static final String FEDERATED_IDENTITIES = "federated_identities";

  // The login.gov's identities field from RAS UserInfo endpoint
  static final String IDENTITIES = "identities";

  // The login.gov's era commons provider name field from RAS UserInfo endpoint
  static final String ERA_COMMONS_PROVIDER_NAME = "era";

  // The login.gov's ther user id field from RAS UserInfo's identity.
  static final String IDENTITY_USERID = "userid";

  // The identifier that indicate users login using login.gov account.
  static final String ID_ME_IDENTIFIER_LOWER_CASE = "@id.me";
  // The identifier that indicate users login using login.gov account.
  static final String LOGIN_GOV_IDENTIFIER_LOWER_CASE = "@login.gov";

  // The string in ACR claim that we can use to identify user IAL status.
  static final String ACR_CLAIM_IAL_2_IDENTIFIER = "/assurance/ial/2";

  // The required scopes to finish RAS OAuth flow, and get enough information AoU needs.
  static final Set<String> RAS_AUTH_CODE_SCOPES =
      ImmutableSet.of("openid", "profile", "federated_identities");
}
