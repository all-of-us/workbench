import { Modifier, SearchParameter, SearchResult } from './';

export class SearchRequest {
  type: string;
  searchParameters: SearchParameter[] = [];
  modifiers: Modifier[] = [];

  constructor(type: string, searchResult: SearchResult) {
    this.type = type;
    this.searchParameters = searchResult.values;
    this.modifiers = searchResult.modifierList;
  }
}
