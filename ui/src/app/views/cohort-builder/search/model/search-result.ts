import { Modifier, Subject, SearchParameter } from './';
import { SearchResponse } from './search-response';
import { Criteria } from '../../../../../generated/model/criteria';

export class SearchResult {

  id: number;
  description: string;
  searchType: string;
  count = 0;
  values: SearchParameter[] = [];
  criteriaList: Criteria[] = [];
  modifierList: Modifier[] = [];
  resultSet: Subject[] = [];

  constructor()

  constructor(description: string, criteriaList: Criteria[], modifierList: Modifier[])

  constructor(description?: string, criteriaList?: Criteria[], modifierList?: Modifier[]) {
    if (description && criteriaList && modifierList) {
      this.update(description, criteriaList, modifierList);
    }
  }

  update(description: string, criteriaList: Criteria[], modifierList: Modifier[]) {
    this.description = description;
    criteriaList ? this.criteriaList = criteriaList : this.criteriaList = [];
    modifierList ? this.modifierList = modifierList : this.modifierList = [];
    this.count = -1;
    for (const criteria of this.criteriaList) {
      this.searchType = criteria.type;
      this.values.push(new SearchParameter(
          criteria.code, criteria.type.startsWith('DEMO') ? criteria.type : criteria.domainId));
    }
  }

  updateWithResponse(response: SearchResponse) {
    this.resultSet = response.subjectList;
    this.count = response.subjectList.length;
  }

  displayValues(): string {
    let displayValues;
    for (const value of this.values) {
      if (displayValues) {
        displayValues = displayValues + ', ' + value.code;
      } else {
        displayValues = value.code;
      }
    }
    return displayValues;
  }

  displayTitle(): string {
    return 'Contains ' + this.searchType + ' Codes';
  }
}
