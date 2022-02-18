import { serverConfigStore } from 'app/utils/stores';

export const updateVisibleTiers = (tiers: string[]) => {
  serverConfigStore.set({
    config: {
      ...serverConfigStore.get().config,
      accessTiersVisibleToUsers: tiers,
    },
  });
};
