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
  @Mapping(target = "userName", source="username")
  User toApiUser(DbUser user);

  User toUser(UserRole userRole);

  @Mapping(source = "acl", target = "role")
  @Mapping(source = "user.username", target = "email")
  @Mapping(source = "user.username", target = "userName")
  UserRole toApiUserRole(DbUser user, RawlsWorkspaceAccessEntry acl);

  @Mapping(source = "dbUser.userId", target = "userDatabaseId")
  @Mapping(source = "dbUser.creationTime", target = "userAccountCreatedTime")
  @Mapping(source = "dbUser", target = "userModel")
  WorkspaceUserAdminView toWorkspaceUserAdminView(DbUser dbUser, UserRole userRole);

  @Mapping(target = "userModel", source="userRole")
  @Mapping(target = "userDatabaseId", ignore = true)
  @Mapping(target = "userAccountCreatedTime", ignore = true)
  WorkspaceUserAdminView toPartialWorkspaceUserAdminView(UserRole userRole);
}
