import { AuthDomainApi } from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class AuthDomainApiStub extends AuthDomainApi {
  constructor() {
    super(undefined);
  }

  public updateUserDisabledStatus(): Promise<Response> {
    return new Promise<Response>((resolve) => {
      resolve(new Response());
    });
  }
}
