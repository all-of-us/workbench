import {
  Criteria, Modifier,
  SearchParameter, SubjectListResponse, Subject
} from 'generated';

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
      this.values.push(<SearchParameter>{
        code: criteria.code,
        domainId: criteria.type.startsWith('DEMO') ? criteria.type : criteria.domainId
      });
    }
  }

  updateWithResponse(response: SubjectListResponse) {
    this.resultSet = response.items;
    this.count = response.items.length;
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
