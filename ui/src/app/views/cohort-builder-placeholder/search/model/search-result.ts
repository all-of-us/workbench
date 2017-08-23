import { Criteria, Modifier, Subject, SearchParameter } from './';
import { CriteriaGroupComponent } from '../criteria-group/criteria-group.component';
import { SearchResponse } from './search-response';

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

  constructor(description: string, criteriaGroup: CriteriaGroupComponent)

  constructor(description?: string, criteriaGroup?: CriteriaGroupComponent) {
    if (description && criteriaGroup) {
      this.update(description, criteriaGroup);
    }
  }

  update(description: string, criteriaGroup: CriteriaGroupComponent) {
    this.description = description;
    criteriaGroup.modifierList ? this.modifierList = criteriaGroup.modifierList : this.modifierList = [];
    criteriaGroup.criteriaList ? this.criteriaList = criteriaGroup.criteriaList : this.criteriaList = [];
    this.count = -1;
    for (const criteria of this.criteriaList) {
      this.searchType = criteria.type;
      this.values.push(new SearchParameter(criteria.code, criteria.type.startsWith('DEMO') ? criteria.type : criteria.domainId));
    }
  }

  updateWithResponse(response: SearchResponse) {
    this.resultSet = response.subjectList;
    this.values = response.searchParameterList;
    this.count = response.subjectList.length;
  }

  displayValues(): string[] {
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
