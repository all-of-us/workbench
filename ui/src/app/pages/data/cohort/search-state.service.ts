import { CohortDefinition } from 'generated/fetch';

import { BehaviorSubject } from 'rxjs/BehaviorSubject';

export const initSearchRequest = {
  includes: [],
  excludes: [],
  dataFilters: [],
} as CohortDefinition;
export const searchRequestStore = new BehaviorSubject<any>(initSearchRequest);
export const idsInUse = new BehaviorSubject<any>(new Set());
export const ppiQuestions = new BehaviorSubject<any>({});
export const ppiSurveys = new BehaviorSubject<any>({});
export const encountersStore = new BehaviorSubject<any>(undefined);
export const criteriaMenuOptionsStore = new BehaviorSubject<any>({});
export const ageCountStore = new BehaviorSubject<any>({});
