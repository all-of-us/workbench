import {
  NihToken,
  ProfileApi
} from 'generated/fetch';

// TODO: Port functionality from ProfileServiceStub as needed.
export class ProfileApiStub extends ProfileApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  public updateNihToken(token?: NihToken, options?: any) {
    return new Promise<Response>(resolve => { resolve(new Response()); });
  }

}
