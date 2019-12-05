package org.pmiops.workbench.firecloud;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;

/** Static utilities relating to transforming Firecloud API responses. */
public final class FirecloudTransforms {
  private FirecloudTransforms() {}

  /**
   * Swagger Java codegen does not handle the WorkspaceACL model correctly; it returns a GSON map
   * instead. Run this through a typed Gson conversion process to coerce it into the desired type.
   */
  public static Map<String, FirecloudWorkspaceAccessEntry> extractAclResponse(
      FirecloudWorkspaceACL aclResp) {
    Type accessEntryType = new TypeToken<Map<String, FirecloudWorkspaceAccessEntry>>() {}.getType();
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType);
  }
}
