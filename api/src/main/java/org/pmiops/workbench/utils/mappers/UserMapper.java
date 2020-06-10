package org.pmiops.workbench.utils.mappers;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceUserAdminView;

@Mapper(componentModel = "spring", uses = FirecloudMapper.class)
public interface UserMapper {

  @Mapping(target = "email", source = "user.username")
  @Mapping(target = "role", source = "acl")
  UserRole toApiUserRole(DbUser user, FirecloudWorkspaceAccessEntry acl);

  @Mapping(source = "contactEmail", target = "email")
  @Mapping(source = "userRole.email", target = "userName")
  @Named("toUserApiModel")
  User toUserApiModel(UserRole userRole, String contactEmail);

  @Mapping(source = "dbUser.userId", target = "userDatabaseId")
  @Mapping(source = "dbUser.creationTime", target = "userAccountCreatedTime")
  @Mapping(target = "userModel", ignore = true) // set in @AfterMapping
  WorkspaceUserAdminView toWorkspaceUserAdminView(DbUser dbUser, UserRole userRole);

  @AfterMapping
  default void setUserModel(@MappingTarget WorkspaceUserAdminView workspaceUserAdminView, DbUser dbUser, UserRole userRole) {
    final User userModel = toUserApiModel(userRole, dbUser.getContactEmail());
    workspaceUserAdminView.setUserModel(userModel);
  }
}
