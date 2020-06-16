import {InstitutionalRole, PublicInstitutionDetails} from "../../generated/fetch";
import {isBlank} from "./index";
import {AccountCreationOptions} from "../pages/login/account-creation/account-creation-options";

export const getRoleOptions = (institutions: Array<PublicInstitutionDetails>, institutionShortName: string):
    Array<{ label: string, value: InstitutionalRole }> => {
  if (isBlank(institutionShortName)) {
    return [];
  }

  const selectedOrgType = institutions.find(
      inst => inst.shortName === institutionShortName).organizationTypeEnum;
  const availableRoles: Array<InstitutionalRole> =
      AccountCreationOptions.institutionalRolesByOrganizationType
      .find(obj => obj.type === selectedOrgType)
          .roles;

  return AccountCreationOptions.institutionalRoleOptions.filter(option =>
      availableRoles.includes(option.value)
  );
};
