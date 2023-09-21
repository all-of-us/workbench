// utilities around Authority and page-based authorization

import { Authority, Profile } from 'generated/fetch';

// Admin actions guarded by a particular Authority
enum AuthorityGuardedAction {
  EGRESS_EVENTS,
  SHOW_ADMIN_MENU,
  USER_ADMIN,
  USER_AUDIT,
  WORKSPACE_ADMIN,
  WORKSPACE_AUDIT,
  SERVICE_BANNER,
  INSTITUTION_ADMIN,
  PUBLISH_WORKSPACE,
}

// The full set of Authorities which guard admin-menu actions
const adminMenuAuthorities: Set<Authority> = new Set([
  Authority.ACCESS_CONTROL_ADMIN,
  Authority.RESEARCHER_DATA_VIEW,
  Authority.COMMUNICATIONS_ADMIN,
  Authority.INSTITUTION_ADMIN,
  Authority.SECURITY_ADMIN,
]);

const authorityByPage: Map<AuthorityGuardedAction, Authority> = new Map([
  [AuthorityGuardedAction.EGRESS_EVENTS, Authority.SECURITY_ADMIN],
  [AuthorityGuardedAction.USER_ADMIN, Authority.ACCESS_CONTROL_ADMIN],
  [AuthorityGuardedAction.USER_AUDIT, Authority.ACCESS_CONTROL_ADMIN],
  [AuthorityGuardedAction.WORKSPACE_ADMIN, Authority.RESEARCHER_DATA_VIEW],
  [AuthorityGuardedAction.WORKSPACE_AUDIT, Authority.RESEARCHER_DATA_VIEW],
  [AuthorityGuardedAction.SERVICE_BANNER, Authority.COMMUNICATIONS_ADMIN],
  [AuthorityGuardedAction.INSTITUTION_ADMIN, Authority.INSTITUTION_ADMIN],
  [
    AuthorityGuardedAction.PUBLISH_WORKSPACE,
    Authority.FEATURED_WORKSPACE_ADMIN,
  ],
]);

const hasAuthorityForAction = (
  profile: Profile,
  action: AuthorityGuardedAction
): boolean => {
  // DEVELOPER is the super-Authority which includes all others
  if (profile.authorities.includes(Authority.DEVELOPER)) {
    return true;
  }

  // return true if we have any of the menu-displaying authorities
  if (action === AuthorityGuardedAction.SHOW_ADMIN_MENU) {
    return profile.authorities.some((auth) => adminMenuAuthorities.has(auth));
  }

  return profile.authorities.includes(authorityByPage.get(action));
};

export { AuthorityGuardedAction, hasAuthorityForAction };
