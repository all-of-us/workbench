export class SearchGroup {
  constructor(public displayMessage = 'Include participants where:',
              public count = 0,
              public results = [],
              public groupSet = []) {}
}
