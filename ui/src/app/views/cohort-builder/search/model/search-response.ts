import { SearchParameter, Subject } from './';

export class SearchResponse {
  subjectList: Subject[] = [];
  searchParameterList: SearchParameter[];
}
