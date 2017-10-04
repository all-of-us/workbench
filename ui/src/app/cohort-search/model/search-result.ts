import {
  Criteria, Modifier,
  SearchParameter, SubjectListResponse,
} from 'generated';

export class SearchResult {
  id: number;
  description: string;
  searchType: string;
  count = 0;
  values: SearchParameter[] = [];
  criteriaList: Criteria[] = [];
  modifierList: Modifier[] = [];
  resultSet: String[] = [];

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
        value: criteria.code,
        domain: criteria.type.startsWith('DEMO') ? criteria.type : criteria.domainId,
        conceptId: criteria.conceptId
      });
    }
  }

  updateWithResponse(response: SubjectListResponse) {
    this.resultSet = response;
    this.count = response.length;
  }

  displayValues(): string {
    let displayValues;
    for (const value of this.values) {
      if (displayValues) {
        displayValues = displayValues + ', ' + value.value;
      } else {
        displayValues = value.value;
      }
    }
    return displayValues;
  }

  displayTitle(): string {
    return 'Contains ' + this.searchType + ' Codes';
  }
}
