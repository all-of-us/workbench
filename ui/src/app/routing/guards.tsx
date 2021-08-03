import {Guard} from "../components/app-router";
import {authStore, profileStore} from "../utils/stores";
import {hasRegisteredAccess} from "../utils/access-tiers";

export const signInGuard: Guard = {
  allowed: (): boolean => {
    return authStore.get().isSignedIn;
  },
  redirectPath: '/login'
};

export const disabledGuard = (userDisabled: boolean): Guard => ({
  allowed: (): boolean => !userDisabled,
  redirectPath: '/user-disabled'
});

export const registrationGuard: Guard = {
  allowed: (): boolean => hasRegisteredAccess(profileStore.get().profile.accessTierShortNames),
  redirectPath: '/'
};

export const expiredGuard: Guard = {
  allowed: (): boolean => !profileStore.get().profile.renewableAccessModules.anyModuleHasExpired,
  redirectPath: '/access-renewal'
};
