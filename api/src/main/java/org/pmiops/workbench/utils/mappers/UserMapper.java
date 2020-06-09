package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.UserRole;

@Mapper(componentModel = "spring", uses = FirecloudMapper.class)
public interface UserMapper {
  @Mapping(source = "acl", target = "role")
  @Mapping(source = "user.username", target = "email")
  UserRole toApiUserRole(DbUser user, FirecloudWorkspaceAccessEntry acl);

  @Mapping(source = "contactEmail", target = "email")
  @Mapping(source = "userRole.email", target = "userName")
  User toUserApiModel(UserRole userRole, String contactEmail);
}
