import { CreateNewUserSatisfactionSurvey, SurveysApi } from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class SurveysApiStub extends SurveysApi {
  constructor() {
    super(undefined);
  }

  createNewUserSatisfactionSurvey(
    _newUserSatisfactionSurvey: CreateNewUserSatisfactionSurvey,
    _options?: any
  ) {
    return Promise.resolve(undefined);
  }
}
