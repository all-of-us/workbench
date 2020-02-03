import {DataAccessLevel, Profile} from 'generated/fetch';

export const getEmptyProfile = (): Profile => {
  return {
    username: '',
    dataAccessLevel: DataAccessLevel.Unregistered,
    givenName: '',
    familyName: '',
    contactEmail: '',
    currentPosition: '',
    organization: '',
    areaOfResearch: '',
    address: {
      streetAddress1: '',
      streetAddress2: '',
      city: '',
      state: '',
      country: '',
      zipCode: '',
    },
    institutionalAffiliations: [
      {
        institution: undefined,
        nonAcademicAffiliation: undefined,
        role: undefined
      }
    ],
    demographicSurvey: {},
    degrees: []
  };
};
