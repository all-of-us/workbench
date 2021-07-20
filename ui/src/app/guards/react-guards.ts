import {hasRegisteredAccess} from "app/utils/access-tiers";
import {authStore, profileStore} from "app/utils/stores";

export interface Guard {
  allowed: () => boolean;
  redirectPath: string;
}

export const expiredGuard: Guard = {
  allowed: (): boolean => !profileStore.get().profile.renewableAccessModules.anyModuleHasExpired,
  redirectPath: '/access-renewal'
};

export const registrationGuard: Guard = {
  allowed: (): boolean => hasRegisteredAccess(profileStore.get().profile.accessTierShortNames),
  redirectPath: '/'
};

export const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};
