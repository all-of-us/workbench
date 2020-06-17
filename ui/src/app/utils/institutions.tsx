import * as fp from 'lodash/fp';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {InstitutionalRole, PublicInstitutionDetails} from 'generated/fetch';
import {isBlank} from './index';

export const getRoleOptions = (institutions: Array<PublicInstitutionDetails>, institutionShortName: string):
    Array<{ label: string, value: InstitutionalRole }> => {
  if (isBlank(institutionShortName)) {
    return [];
  }

  const institution = fp.find(institution => {
    const {shortName} = institution;
    return shortName === institutionShortName;
  }, institutions);
  const {organizationTypeEnum} = institution;
  const availableRoles: Array<InstitutionalRole> =
      AccountCreationOptions.institutionalRolesByOrganizationType
      .find(obj => obj.type === organizationTypeEnum)
          .roles;

  return AccountCreationOptions.institutionalRoleOptions.filter(option =>
      availableRoles.includes(option.value)
  );
};
