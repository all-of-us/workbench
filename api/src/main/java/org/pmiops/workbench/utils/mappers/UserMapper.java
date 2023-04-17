package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, FirecloudMapper.class})
public interface UserMapper {
  @Mapping(source = "acl", target = "role")
  @Mapping(source = "user.username", target = "email")
  UserRole toApiUserRole(DbUser user, RawlsWorkspaceAccessEntry acl);

  @Mapping(source = "contactEmail", target = "email")
  @Mapping(source = "userRole.email", target = "userName")
  User toApiUser(UserRole userRole, String contactEmail);

  @Mapping(source = "contactEmail", target = "email")
  @Mapping(source = "username", target = "userName")
  User toApiUser(DbUser dbUser);

  @Mapping(source = "dbUser.userId", target = "userDatabaseId")
  @Mapping(source = "dbUser.creationTime", target = "userAccountCreatedTime")
  @Mapping(source = "dbUser", target = "userModel")
  WorkspaceUserAdminView toWorkspaceUserAdminView(DbUser dbUser, UserRole userRole);
}
