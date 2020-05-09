package org.pmiops.workbench.profile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface AddressMapper {
  Address dbAddressToAddress(DbAddress dbAddress);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true) // set by UserService.createUser
  DbAddress addressToDbAddress(Address address);
}
