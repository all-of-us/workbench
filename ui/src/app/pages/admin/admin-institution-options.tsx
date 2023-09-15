import {
  InstitutionMembershipRequirement,
  OrganizationType,
} from 'generated/fetch';

export const MembershipRequirements = [
  {
    label: 'Email address is at one of the following domains:',
    value: InstitutionMembershipRequirement.DOMAINS,
  },
  {
    label: 'Individual email address is listed below:',
    value: InstitutionMembershipRequirement.ADDRESSES,
  },
];
export const OrganizationTypeOptions = [
  { label: 'Industry', value: OrganizationType.INDUSTRY },
  {
    label: 'Academic Research Institution',
    value: OrganizationType.ACADEMIC_RESEARCH_INSTITUTION,
  },
  {
    label: 'Educational Institution',
    value: OrganizationType.EDUCATIONALINSTITUTION,
  },
  {
    label: 'Health Center / Non-Profit',
    value: OrganizationType.HEALTH_CENTER_NON_PROFIT,
  },
  { label: 'Other', value: OrganizationType.OTHER },
];
