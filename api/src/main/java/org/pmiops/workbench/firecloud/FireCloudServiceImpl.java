package org.pmiops.workbench.firecloud;

import com.google.api.client.http.HttpMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.apache.http.client.HttpClient;
import org.pmiops.workbench.firecloud.Entity.EntityTypes;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.model.Me;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FireCloudServiceImpl implements FireCloudService {

  private static final ImmutableList<Pair> NO_QUERY_PARAMS = ImmutableList.of();
  private static final ImmutableMap<String, String> HEADERS =
      ImmutableMap.of("Accept", "application/json");
  private static final ImmutableMap<String, Object> NO_FORM_PARAMS = ImmutableMap.of();
  private static final String[] AUTH_NAMES = new String[] { "googleoauth" };

  private final Provider<ProfileApi> profileApiProvider;

  // Swagger-generated APIs don't work with FireCloud entities; use ApiClient directly.
  private final Provider<ApiClient> apiClientProvider;

  @Autowired
  public FireCloudServiceImpl(Provider<ProfileApi> profileApiProvider,
      Provider<ApiClient> apiClientProvider) {
    this.profileApiProvider = profileApiProvider;
    this.apiClientProvider = apiClientProvider;
  }

  @Override
  public boolean isRequesterEnabledInFirecloud() throws ApiException {
    ProfileApi profileApi = profileApiProvider.get();
    Me me = profileApi.me();
    // Users can only use FireCloud if the Google and LDAP flags are enabled.
    return me.getEnabled() != null
        && isTrue(me.getEnabled().getGoogle()) && isTrue(me.getEnabled().getLdap());
  }

  private <T> T call(String path, String method, @Nullable String body,
      Type type) throws ApiException {
    try {
      Response response = apiClientProvider.get().buildCall(path, method, NO_QUERY_PARAMS, body,
          HEADERS, NO_FORM_PARAMS, AUTH_NAMES, null).execute();
      if (response.isSuccessful()) {
        return new GsonBuilder().create().fromJson(response.body().string(), type);
      } else {
        String respBody = null;
        if (response.body() != null) {
          try {
            respBody = response.body().string();
          } catch (IOException e) {
            throw new ApiException(response.message(), e, response.code(), response.headers().toMultimap());
          }
        }
        throw new ApiException(response.message(), response.code(), response.headers().toMultimap(), respBody);
      }
    } catch (IOException e) {
      throw new ApiException(e);
    }
  }

  private static String getEntitiesPath(String workspaceNamespace, String workspaceId,
      String entityType) {
    return String.format("/workspaces/{0}/{1}/entities/{2}", workspaceNamespace, workspaceId,
        entityType);
  }

  @Override
  public List<Entity> getEntitiesInWorkspace(String workspaceNamespace, String workspaceId,
      String entityType) throws ApiException {
    return call(getEntitiesPath(workspaceNamespace, workspaceId, entityType),
          HttpMethods.GET, null, new TypeToken<List<Entity>>(){}.getType());
  }

  @Override
  public Entity createEntity(String workspaceNamespace, String workspaceId, Entity entity) {
    return null;
  }

  @Override
  public Entity getEntity(String workspaceNamespace, String workspaceId, String entityType,
      String entityId) {
    return null;
  }

  @Override
  public Entity updateEntity(String workspaceNamespace, String workspaceId, Entity entity) {
    return null;
  }

  @Override
  public void deleteEntity(String workspaceNamespace, String workspaceId, String entityType,
      String entityId) {

  }

  private boolean isTrue(Boolean b) {
    return b != null && b == true;
  }
}
