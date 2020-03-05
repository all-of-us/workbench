package org.pmiops.workbench.profile;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(componentModel = "spring", uses = {CommonMappers.class})
public interface ProfileMapper {
  Profile dbUserToProfile(DbUser dbUser);

  DbUser profileToDbUser(Profile profile);
}
