package org.pmiops.workbench.firecloud;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdate;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

/** Static utilities relating to transforming Firecloud API responses. */
public final class FirecloudTransforms {
  private FirecloudTransforms() {}

  /**
   * Swagger Java codegen does not handle the WorkspaceACL model correctly; it returns a GSON map
   * instead. Run this through a typed Gson conversion process to coerce it into the desired type.
   */
  public static Map<String, RawlsWorkspaceAccessEntry> extractAclResponse(
      RawlsWorkspaceACL aclResp) {
    Type accessEntryType = new TypeToken<Map<String, RawlsWorkspaceAccessEntry>>() {}.getType();
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType);
  }

  public static RawlsWorkspaceACLUpdate buildAclUpdate(
      String email, WorkspaceAccessLevel updatedAccess) {
    RawlsWorkspaceACLUpdate update = new RawlsWorkspaceACLUpdate().email(email);
    if (updatedAccess == WorkspaceAccessLevel.OWNER) {
      return update
          .canShare(true)
          .canCompute(true)
          .accessLevel(WorkspaceAccessLevel.OWNER.toString());
    } else if (updatedAccess == WorkspaceAccessLevel.WRITER) {
      return update
          .canShare(false)
          .canCompute(true)
          .accessLevel(WorkspaceAccessLevel.WRITER.toString());
    } else if (updatedAccess == WorkspaceAccessLevel.READER) {
      return update
          .canShare(false)
          .canCompute(false)
          .accessLevel(WorkspaceAccessLevel.READER.toString());
    } else {
      return update
          .canShare(false)
          .canCompute(false)
          .accessLevel(WorkspaceAccessLevel.NO_ACCESS.toString());
    }
  }
}
