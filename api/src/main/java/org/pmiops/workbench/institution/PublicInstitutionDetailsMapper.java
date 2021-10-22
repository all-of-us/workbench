package org.pmiops.workbench.institution;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.MembershipRequirement;
import org.pmiops.workbench.model.PublicInstitutionDetails;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface PublicInstitutionDetailsMapper {
  PublicInstitutionDetails dbToModel(
      DbInstitution dbObject, MembershipRequirement registeredTierMembershipRequirement);
}
