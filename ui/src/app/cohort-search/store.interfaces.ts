import {SearchRequest, SubjectListResponse as SubjectList, SearchGroupItem} from 'generated';


export type SearchGroupRole = 'include' | 'exclude';

export interface AppContext {
  wizardOpen: boolean;
  active?: {
    criteria: string;
    sgIndex: number;
    sgRole: SearchGroupRole;
    sgItemIndex?: number;
  };
}

export interface CohortSearchState {
  search: Object;
  subjects?: SubjectList;
  context?: AppContext;
  criteriaTree?: object;
  loading?: object;
}
