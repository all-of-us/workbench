import {InstitutionMembershipRequirement, OrganizationType} from 'generated/fetch';

export const MembershipRequirements =  [{label: 'Master', value: InstitutionMembershipRequirement.DOMAINS},
  {label: 'Individual', value: InstitutionMembershipRequirement.ADDRESSES}];
export const OrganizationTypeOptions = [
  {label: 'Industry', value: OrganizationType.INDUSTRY},
  {label: 'Academic Research Institution', value: OrganizationType.ACADEMICRESEARCHINSTITUTION},
  {label: 'Educational Institution', value: OrganizationType.EDUCATIONALINSTITUTION},
  {label: 'Health Center / Non-Profit', value: OrganizationType.HEALTHCENTERNONPROFIT},
  {label: 'Other', value: OrganizationType.OTHER}
];
