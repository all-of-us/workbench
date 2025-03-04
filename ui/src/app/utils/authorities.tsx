// utilities around Authority and page-based authorization

import { Authority, Profile } from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';

// Admin actions guarded by a particular Authority
export enum AuthorityGuardedAction {
  EGRESS_EVENTS,
  EGRESS_BYPASS,
  SHOW_ADMIN_MENU,
  USER_ADMIN,
  USER_AUDIT,
  WORKSPACE_ADMIN,
  WORKSPACE_AUDIT,
  SERVICE_BANNER,
  INSTITUTION_ADMIN,
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
  [AuthorityGuardedAction.EGRESS_BYPASS, Authority.SECURITY_ADMIN],
  [AuthorityGuardedAction.USER_ADMIN, Authority.ACCESS_CONTROL_ADMIN],
  [AuthorityGuardedAction.USER_AUDIT, Authority.ACCESS_CONTROL_ADMIN],
  [AuthorityGuardedAction.WORKSPACE_ADMIN, Authority.RESEARCHER_DATA_VIEW],
  [AuthorityGuardedAction.WORKSPACE_AUDIT, Authority.RESEARCHER_DATA_VIEW],
  [AuthorityGuardedAction.SERVICE_BANNER, Authority.COMMUNICATIONS_ADMIN],
  [AuthorityGuardedAction.INSTITUTION_ADMIN, Authority.INSTITUTION_ADMIN],
]);

export const hasAuthorityForAction = (
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

// incomplete.  please add as needed.
export const noAccessText = (action: AuthorityGuardedAction) => {
  const actionDescription = switchCase(
    action,
    [AuthorityGuardedAction.EGRESS_BYPASS, () => 'make egress bypass requests'],
    [AuthorityGuardedAction.EGRESS_EVENTS, () => 'view egress events']
  );
  return `You do not have permission to ${actionDescription}. ${authorityByPage.get(
    action
  )} authority is required.`;
};

export const renderIfAuthorized = (
  profile: Profile,
  action: AuthorityGuardedAction,
  render: () => JSX.Element
): JSX.Element =>
  hasAuthorityForAction(profile, action) ? (
    render()
  ) : (
    <div>{noAccessText(action)}</div>
  );
