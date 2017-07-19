package org.pmiops.workbench.firecloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class FireCloudConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient fireCloudApiClient(UserAuthentication userAuthentication) {
    ApiClient apiClient = new ApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ProfileApi profileApi(ApiClient apiClient) {
    ProfileApi api = new ProfileApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  public Gson gson() {
    return new GsonBuilder().registerTypeAdapter(Entity.class, new JsonDeserializer<Entity>() {
      @Override
      public Entity deserialize(JsonElement element, Type typeOfT,
          JsonDeserializationContext context)
          throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        return new Entity(jsonObject.get(NAME_ATTRIBUTE).getAsString())
      }
    });
  }
}
