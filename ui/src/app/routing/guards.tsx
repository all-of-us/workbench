import { matchPath } from 'react-router-dom';

import { AccessModule, Profile } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { Guard } from 'app/components/app-router';
import { UIAppType } from 'app/components/apps-panel/utils';
import {
  AccessTierShortNames,
  hasRegisteredTierAccess,
} from 'app/utils/access-tiers';
import {
  ACCESS_RENEWAL_PATH,
  DATA_ACCESS_REQUIREMENTS_PATH,
  eligibleForTier,
  getAccessModuleStatusByNameOrEmpty,
  hasRtExpired,
  isBypassed,
  isCompleted,
} from 'app/utils/access-utils';
import {
  AuthorityGuardedAction,
  hasAuthorityForAction,
} from 'app/utils/authorities';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { shouldShowDemographicSurvey } from 'app/utils/profile-utils';
import { authStore, MatchParams, profileStore } from 'app/utils/stores';

import { AuthorityMissing } from './authority-missing';
import { analysisTabName, analysisTabPath, workspacePath } from './utils';

export const signInGuard: Guard = {
  allowed: (): boolean => {
    return authStore.get().isSignedIn;
  },
  redirectPath: '/login' + window.location.search,
};

export const userIsDisabled = (userDisabledInDb: boolean) =>
  userDisabledInDb ||
  !eligibleForTier(profileStore.get().profile, AccessTierShortNames.Registered);

export const disabledGuard = (userDisabledInDb: boolean): Guard => ({
  // Show disabled screen when user account is disabled by admin or removed from institution registered tier requirement.
  allowed: (): boolean => !userIsDisabled(userDisabledInDb),
  redirectPath: '/user-disabled' + window.location.search,
});

export const userDisabledPageGuard = (userDisabledInDb: boolean): Guard => ({
  // enabled users should be redirected to the homepage if they visit the /user-disabled page
  allowed: (): boolean => userIsDisabled(userDisabledInDb),
  redirectPath: '/',
});

const allCompleteOrBypassed = (
  profile: Profile,
  moduleNames: AccessModule[]
) => {
  const modules = profile?.accessModules?.modules;
  return moduleNames.every((moduleName) => {
    const status = getAccessModuleStatusByNameOrEmpty(modules, moduleName);
    return (
      isCompleted(status, profile?.duccSignedVersion) || isBypassed(status)
    );
  });
};

// use this for all access-module routing decisions, to ensure only one routing is chosen
export const shouldRedirectToMaybe = (profile: Profile): string | undefined => {
  return cond(
    [hasRtExpired(profile), () => ACCESS_RENEWAL_PATH],
    // not a common scenario (mainly test users) but AAR is the only way to recover if these modules are missing
    [
      !allCompleteOrBypassed(profile, [
        AccessModule.PROFILE_CONFIRMATION,
        AccessModule.PUBLICATION_CONFIRMATION,
      ]),
      () => ACCESS_RENEWAL_PATH,
    ],
    [!hasRegisteredTierAccess(profile), () => DATA_ACCESS_REQUIREMENTS_PATH]
    // by default: no redirect
  );
};

// use this for all access-module routing decisions, to ensure only one routing is chosen
export const getAccessModuleGuard = (): Guard => {
  const redirectPath = shouldRedirectToMaybe(profileStore.get().profile);
  return {
    allowed: () => !redirectPath,
    redirectPath,
  };
};

export const adminLockedGuard = (ns: string, wsid: string): Guard => {
  return {
    allowed: (): boolean => !currentWorkspaceStore.getValue().adminLocked,
    redirectPath: `${workspacePath(ns, wsid)}/about`,
  };
};

export const confirmAppIsValid = () => {
  const urlMatchParam = matchPath<MatchParams>(location.pathname, {
    path: `/workspaces/:ns/:wsid/${analysisTabName}/userApp/:appType`,
  });
  const appFromUrl = urlMatchParam.params.appType as UIAppType;
  return Object.values(UIAppType).includes(appFromUrl);
};

export const appIsValidGuard = (ns: string, wsid: string): Guard => {
  return {
    allowed: (): boolean => confirmAppIsValid(),
    redirectPath: `${analysisTabPath(ns, wsid)}`,
  };
};

export const authorityGuard = (
  guardedAction: AuthorityGuardedAction
): Guard => ({
  allowed: () =>
    hasAuthorityForAction(profileStore.get().profile, guardedAction),
  renderBlocked: () => <AuthorityMissing action={guardedAction} />,
});

export const restrictDemographicSurvey = (): Guard => ({
  allowed: () => shouldShowDemographicSurvey(profileStore.get().profile),
  redirectPath: '/',
});
