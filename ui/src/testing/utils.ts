import { AppStatus, TerraJobStatus } from 'generated/fetch';

export function minus<T>(a1: T[], a2: T[]): T[] {
  return a1.filter((e) => !a2.includes(e));
}

export const ALL_GKE_APP_STATUSES = Object.keys(AppStatus)
  .map((k) => AppStatus[k])
  .concat([null, undefined]);

export const ALL_TERRA_JOB_STATUSES = Object.keys(TerraJobStatus)
  .map((k) => TerraJobStatus[k])
  .concat([null, undefined]);
