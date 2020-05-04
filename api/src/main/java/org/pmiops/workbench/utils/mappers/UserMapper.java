package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.UserRole;

@Mapper(componentModel = "spring", uses = FirecloudMapper.class)
public interface UserMapper {
  @Mapping(target = "email", source = "user.username")
  @Mapping(target = "role", source = "acl")
  UserRole toApiUserRole(DbUser user, FirecloudWorkspaceAccessEntry acl);

  @Mapping(source = "contactEmail", target = "email")
  @Mapping(source = "userRole.email", target = "userName")
  User toUserApiModel(UserRole userRole, String contactEmail);
}
