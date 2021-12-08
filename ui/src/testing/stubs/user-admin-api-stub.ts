import { AccessBypassRequest, AdminUserListResponse, EmptyResponse, Profile, UserAdminApi } from "generated/fetch";
import { ProfileStubVariables } from "./profile-api-stub";
import { stubNotImplementedError } from "./stub-utils";

export class UserAdminApiStub extends UserAdminApi {
  constructor(public profile = ProfileStubVariables.PROFILE_STUB) {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
  }

  public bypassAccessRequirement(
    userId: number, bypassed?: AccessBypassRequest, options?: any): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(() => {});
  }


  public getUser(userId: number): Promise<Profile> {
    return Promise.resolve(this.profile);
  }

  public getAllUsers(): Promise<AdminUserListResponse> {
    return Promise.resolve({users: [{
      userId: 1,
      username: ProfileStubVariables.PROFILE_STUB.username,
    }]});
  }
}
