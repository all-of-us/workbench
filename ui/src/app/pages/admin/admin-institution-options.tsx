import {DuaType, OrganizationType} from 'generated/fetch';

export const DuaTypes =  [{label: 'Master', value: DuaType.MASTER},
  {label: 'Individual', value: DuaType.RESTRICTED}];

export const OrganizationTypeOptions = [
  {label: 'Industry', value: OrganizationType.INDUSTRY},
  {label: 'Academic Research Institution', value: OrganizationType.ACADEMICRESEARCHINSTITUTION},
  {label: 'Educational Institution', value: OrganizationType.EDUCATIONALINSTITUTION},
  {label: 'Health Center/ Non-Profit', value: OrganizationType.HEALTHCENTERNONPROFIT},
  {label: 'Other', value: OrganizationType.OTHER}
];
