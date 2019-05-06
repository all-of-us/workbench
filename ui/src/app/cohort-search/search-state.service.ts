import {SearchRequest} from 'generated/fetch';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

export const initSearchRequest = {includes: [], excludes: []} as SearchRequest;
export const searchRequestStore = new BehaviorSubject<any>(initSearchRequest);
export const selectionsStore = new BehaviorSubject<any>([]);
export const groupSelectionsStore = new BehaviorSubject<any>([]);
export const wizardStore = new BehaviorSubject<any>(undefined);
export const subtreePathStore = new BehaviorSubject<any>([]);
export const subtreeSelectedStore = new BehaviorSubject<any>(undefined);
export const scrollStore = new BehaviorSubject<any>(undefined);
export const attributesStore = new BehaviorSubject<any>(undefined);
export const autocompleteStore = new BehaviorSubject<any>('');
