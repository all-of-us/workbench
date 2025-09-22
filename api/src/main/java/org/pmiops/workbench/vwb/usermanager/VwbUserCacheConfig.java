package org.pmiops.workbench.vwb.usermanager;

import org.springframework.context.annotation.Bean;
import org.springframework.web.context.annotation.RequestScope;

public class VwbUserCacheConfig {
  public static final String VWB_USER_CACHE_REQUEST_SCOPED_GROUP_CACHE = "firecloudGroupCache";

  /**
   * @return a request scoped cache of firecloud groups, by group name. Groups are lazily populated
   *     as requested in the cache, and will expire after a duration. Lazy fetch is authenticated as
   *     the service, so this should only be used for trusted internal checks (not on arbitrary user
   *     inputs).
   */
  @Bean(name = VWB_USER_CACHE_REQUEST_SCOPED_GROUP_CACHE)
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
