package org.pmiops.workbench.firecloud;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class FireCloudCacheConfig {
  public static final String SERVICE_ACCOUNT_REQUEST_SCOPED_GROUP_CACHE = "firecloudGroupCache";

  /**
   * @return a request scoped cache of firecloud groups, by group name. Groups are lazily populated
   *     as requested in the cache, and will expire after a duration. Lazy fetch is authenticated as
   *     the service, so this should only be used for trusted internal checks (not on arbitrary user
   *     inputs).
   */
  @Bean(name = SERVICE_ACCOUNT_REQUEST_SCOPED_GROUP_CACHE)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public LoadingCache<String, FirecloudManagedGroupWithMembers> firecloudGroupCache(
      final @Qualifier(FireCloudConfig.SERVICE_ACCOUNT_GROUPS_API) GroupsApi groupsApi) {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        // Set a small limit, as groups are large, and our system doesn't use many. If we start
        // using groups more broadly, this limit could be reconsidered.
        .maximumSize(10)
        .build(
            new CacheLoader<String, FirecloudManagedGroupWithMembers>() {
              @Override
              public FirecloudManagedGroupWithMembers load(String groupName) throws Exception {
                return groupsApi.getGroup(groupName);
              }
            });
  }
}
