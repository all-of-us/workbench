/**
 * NgModule constants
 */

/* Criteria categories */
export const CRITERIA_TYPES = [
  { id: 1, name: 'Demographics', type: 'demo' },
  { id: 2, name: 'ICD9 Codes', type: 'icd9' },
  { id: 3, name: 'ICD10 Codes', type: 'icd10' },
  { id: 4, name: 'PheCodes', type: 'phecodes' },
  { id: 5, name: 'CPT Codes', type: 'cpt' },
  { id: 6, name: 'Medications', type: 'meds' },
  { id: 7, name: 'Labs', type: 'labs' },
  { id: 8, name: 'Vitals', type: 'vitals' },
  { id: 8, name: 'Temporal', type: 'temporal' }
];

/* Modifier data */
export const AGE_AT_EVENT: string[] =
  ['Any', 'GTE >=', 'LTE <=', 'Between'];
export const EVENT_DATE: string[] =
  ['Any', 'Within x year(s)', 'GTE >=', 'LTE <=', 'Between'];
export const VISIT_TYPE: string[] =
  ['Any', 'Inpatient visit', 'Outpatient visit'];
export const DAYS_OR_YEARS: string[] =
  ['Days', 'Years'];
export const HAS_OCCURRENCES: string[] =
  ['Any', '1 or more', 'within x days/years', 'x days/years apart'];
