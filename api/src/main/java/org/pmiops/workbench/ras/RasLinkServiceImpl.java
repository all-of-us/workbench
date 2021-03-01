package org.pmiops.workbench.ras;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RasLinkServiceImpl implements RasLinkService{

//  private ClientRegistration clientRegistration;
//  private OAuth2AuthorizationRequest authorizationRequest;
//  private OAuth2AuthorizationResponse authorizationResponse;
//  private OAuth2AuthorizationExchange authorizationExchange;
//  //private OAuth2AccessTokenResponseClient<> accessTokenResponseClient;
//  private OAuth2AccessTokenResponse accessTokenResponse;
//  // private OAuth2UserService<oidcuserrequest, oidcuser=""> userService;
//  private OidcAuthorizationCodeAuthenticationProvider authenticationProvider;
//private DefaultClientCredentialsTokenResponseClient tokenResponseClient;
//private DefaultOAuth2UserService defaultOAuth2UserService;
//  @Autowired
//  RasLinkServiceImpl(DefaultClientCredentialsTokenResponseClient tokenResponseClient) {
//    this.tokenResponseClient = tokenResponseClient;
//  }

  @Override
  public void getLink(String authCode) {
//    OidcAuthorizationCodeAuthenticationProvider authenticationProvider
//        = new  OidcAuthorizationCodeAuthenticationProvider(accessTokenResponseClient(), userService());
//    clientRegistration().clientId("client1").build();
//    Set<String> scopes = ImmutableSet.of("openid", "email", "profile", "ga4gh_passport_v1");

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    //headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    String body = "grant_type=authorization_code&scope=ga4gh_passport_v1"
        + "&client_id=e5c5d714-d597-48c8-b564-a249d729d0c9"
        + "&client_secret=SECRET&"
        + "&redirect_uri=http%3A%2F%2Flocalhost%3A4200%2Fras"
        + "&code=4ed11c18-bb53-4e4b-9fa8-cdd3894edd8a";
   HttpEntity<String> request = new HttpEntity<>(body, headers);
    String access_token_url = "https://stsstg.nih.gov/auth/oauth/v2/token";
    ResponseEntity<String> result;
    try {
      result = restTemplate.postForEntity(access_token_url, request, String.class);
      System.out.println("~~~~~`result");
      System.out.println(result);
    } catch (Exception e) {
      System.out.println("~~~~~`resultbadbad");
      System.out.println(e.getMessage());
      System.out.println(e);
    }

  }

//  public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient(){
//    DefaultAuthorizationCodeTokenResponseClient accessTokenResponseClient =
//        new DefaultAuthorizationCodeTokenResponseClient();
//
//    OAuth2AccessTokenResponseHttpMessageConverter tokenResponseHttpMessageConverter =
//        new OAuth2AccessTokenResponseHttpMessageConverter();
//    RestTemplate restTemplate = new RestTemplate(Arrays.asList(
//        new FormHttpMessageConverter(), tokenResponseHttpMessageConverter));
//    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
//
//    accessTokenResponseClient.setRestOperations(restTemplate);
//    return accessTokenResponseClient;
//  }
//
//  public OAuth2UserService<OidcUserRequest, OidcUser> userService(){
//    final OidcUserService delegate = new OidcUserService();
//    return (userRequest) -> {
//      // Delegate to the default implementation for loading a user
//      OidcUser oidcUser = delegate.loadUser(userRequest);
//
//      //OAuth2AccessToken accessToken = userRequest.getAccessToken();
//      //Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
//
//      // TODO
//      // 1) Fetch the authority information from the protected resource using accessToken
//      // 2) Map the authority information to one or more GrantedAuthority's and add it to mappedAuthorities
//
//      // 3) Create a copy of oidcUser but use the mappedAuthorities instead
//      //oidcUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
//
//      return oidcUser;
//    };
//  }
}
