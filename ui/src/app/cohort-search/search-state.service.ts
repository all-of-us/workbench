import {SearchRequest} from 'generated/fetch';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

export const initSearchRequest = {includes: [], excludes: []} as SearchRequest;
export const searchRequestStore = new BehaviorSubject<SearchRequest>(initSearchRequest);
export const wizardStore = new BehaviorSubject<any>(undefined);
