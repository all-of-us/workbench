import { Criteria } from 'generated';
import { SearchParameter } from './search-parameter';

export class SearchCriteria implements Criteria {
  constructor(public id = 0,
              public name = '',
              public type = '',
              public code = '',
              public count = 0,
              public group = false,
              public selectable = false,
              public domainId = '',
              public searchParameters: SearchParameter[] = []) {}
}
