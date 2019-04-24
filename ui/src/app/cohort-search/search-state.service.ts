import {SearchRequest} from 'generated/fetch';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

export const initSearchRequest = {includes: [], excludes: []} as SearchRequest;
export const searchRequestStore = new BehaviorSubject<any>(initSearchRequest);
export const selectionsStore = new BehaviorSubject<any>([]);
export const wizardStore = new BehaviorSubject<any>(undefined);
