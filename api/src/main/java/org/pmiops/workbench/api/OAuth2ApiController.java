package org.pmiops.workbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.CloudStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

@Controller
public class OAuth2ApiController {
  static final String AOU_TOKEN_ENDPOINT = "/oauth2/token";
  static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
  private static final String CLIENT_SECRET_PARAM = "client_secret";
  private final RestTemplate restTemplate;
  private final CloudStorageClient cloudStorageClient;

  @Bean
  public static RestTemplate restTemplate() {
    return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
  }

  @Autowired
  public OAuth2ApiController(CloudStorageClient cloudStorageClient, RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.cloudStorageClient = cloudStorageClient;
  }

  @RequestMapping(
      value = AOU_TOKEN_ENDPOINT,
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> proxyOauthToken(HttpServletRequest request) {
    String queryString =
        StringUtils.isEmpty(request.getQueryString()) ? "" : "?" + request.getQueryString();
    String actualEndpoint = GOOGLE_TOKEN_ENDPOINT + queryString;

    String requestBody;
    try (BufferedReader reader = request.getReader()) {
      requestBody = addClientSecret(reader.lines().collect(Collectors.joining()));
    } catch (IOException e) {
      throw new BadRequestException("Could not process token request", e);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    return restTemplate.exchange(
        actualEndpoint, HttpMethod.POST, new HttpEntity<>(requestBody, headers), JsonNode.class);
  }

  private String addClientSecret(String requestBody) {
    List<NameValuePair> parameters = URLEncodedUtils.parse(requestBody, StandardCharsets.UTF_8);

    parameters.add(
        new BasicNameValuePair(
            CLIENT_SECRET_PARAM, cloudStorageClient.getGoogleOAuthClientSecret()));

    return URLEncodedUtils.format(parameters, StandardCharsets.UTF_8);
  }
}
