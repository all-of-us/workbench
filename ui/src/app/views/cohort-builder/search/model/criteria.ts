export class Criteria {
  constructor(public id = 0,
              public name = '',
              public type = '',
              public code = '',
              public count = 0,
              public group = false,
              public selectable = false,
              public hasChildren = false,
              public domainId = '',
              public children = []) {}
}
