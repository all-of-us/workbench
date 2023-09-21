import { AuthDomainApi } from 'generated/fetch';

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
