import { EgressEventStatus } from 'generated/fetch';

// Egress events statuses that can be mutated by an admin.
export const mutableEgressEventStatuses = [
  EgressEventStatus.REMEDIATED,
  EgressEventStatus.VERIFIED_FALSE_POSITIVE,
];
