import {AuthDomainApi, UpdateDisabledStatusForUsersRequest} from 'generated/fetch';


export class AuthDomainApiStub extends AuthDomainApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  public updateDisabledStatusForUsers(request?: UpdateDisabledStatusForUsersRequest): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }
}
