import {
  InvitationVerificationRequest,
  ProfileApi
} from 'generated/fetch';

// TODO: Port functionality from ProfileServiceStub as needed.
export class ProfileApiStub extends ProfileApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  public invitationKeyVerification(request?: InvitationVerificationRequest, options?: any) {
    if (request.invitationKey === 'dummy') {
      const mockResponse = new Response(JSON.stringify({result: 'valid'}), {status: 200})
      return Promise.resolve(mockResponse);
    } else {
      const err = new Error('Invalid invitation code');
      return Promise.reject(response => { throw err; });
    }
  }
}
