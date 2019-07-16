import {AuthDomainApi, EmptyResponse, UpdateUserDisabledRequest} from 'generated/fetch';


export class AuthDomainApiStub extends AuthDomainApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  public updateUserDisabledStatus(request?: UpdateUserDisabledRequest): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }
}
