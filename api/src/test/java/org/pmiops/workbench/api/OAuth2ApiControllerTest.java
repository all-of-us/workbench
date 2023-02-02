package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.http.HttpMethods;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.CloudStorageClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestTemplate;

@SpringJUnitConfig
class OAuth2ApiControllerTest {
  @SpyBean private OAuth2ApiController oAuth2ApiController;

  @MockBean CloudStorageClient cloudStorageClient;
  @MockBean private HttpServletRequest mockHttpServletRequest;
  @MockBean private RestTemplate mockRestTemplate;

  @TestConfiguration
  @Import({OAuth2ApiController.class})
  public static class Config {}

  @BeforeEach
  public void setup() throws IOException {
    when(mockHttpServletRequest.getMethod()).thenReturn(HttpMethods.POST);
    when(mockHttpServletRequest.getReader()).thenReturn(new BufferedReader(new StringReader("")));
  }

  @Test
  void testProxyOauthToken_proxiesQueryString() {
    when(mockHttpServletRequest.getQueryString()).thenReturn("abc=123");
    String expectedUri = String.format("%s?abc=123", OAuth2ApiController.GOOGLE_TOKEN_ENDPOINT);

    oAuth2ApiController.proxyOauthToken(mockHttpServletRequest);

    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(mockRestTemplate).exchange(argument.capture(), any(), any(), eq(JsonNode.class));
    assertThat(argument.getValue()).isEqualTo(expectedUri);
  }

  @Test
  void testProxyOauthToken_proxiesQueryString_empty() {
    when(mockHttpServletRequest.getQueryString()).thenReturn("");
    String expectedUri = OAuth2ApiController.GOOGLE_TOKEN_ENDPOINT;

    oAuth2ApiController.proxyOauthToken(mockHttpServletRequest);

    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(mockRestTemplate).exchange(argument.capture(), any(), any(), eq(JsonNode.class));
    assertThat(argument.getValue()).isEqualTo(expectedUri);
  }

  @Test
  void testProxyOauthToken_proxiesQueryString_null() {
    when(mockHttpServletRequest.getQueryString()).thenReturn(null);
    String expectedUri = OAuth2ApiController.GOOGLE_TOKEN_ENDPOINT;

    oAuth2ApiController.proxyOauthToken(mockHttpServletRequest);

    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(mockRestTemplate).exchange(argument.capture(), any(), any(), eq(JsonNode.class));
    assertThat(argument.getValue()).isEqualTo(expectedUri);
  }

  @Test
  void testProxyOauthToken_addsClientSecretToBody() throws IOException {
    String body = "abc=123";
    when(mockHttpServletRequest.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
    String secretValue = "xyz";
    when(cloudStorageClient.getGoogleOAuthClientSecret()).thenReturn(secretValue);
    String expectedProxiedBody = "abc=123&client_secret=xyz";

    oAuth2ApiController.proxyOauthToken(mockHttpServletRequest);

    ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
    verify(mockRestTemplate).exchange(anyString(), any(), argument.capture(), eq(JsonNode.class));
    assertThat(argument.getValue().getBody()).isEqualTo(expectedProxiedBody);
  }

  @Test
  void testProxyOauthToken_handlesIOExceptions() throws IOException {
    when(mockHttpServletRequest.getReader()).thenThrow(new IOException());

    assertThrows(
        BadRequestException.class,
        () -> oAuth2ApiController.proxyOauthToken(mockHttpServletRequest));
  }

  @Test
  void testProxyOauthToken_forwardsResponse() {
    ObjectNode responseJson = JsonNodeFactory.instance.objectNode();
    when(mockRestTemplate.exchange(anyString(), any(), any(), eq(JsonNode.class)))
        .thenReturn(ResponseEntity.ok(responseJson));

    ResponseEntity<JsonNode> response = oAuth2ApiController.proxyOauthToken(mockHttpServletRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(responseJson);
  }
}
