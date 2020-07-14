import {
  AuditAction,
  AuditEventBundle,
  AuditEventBundleHeader,
  AuditTargetPropertyChange
} from 'generated';
import * as fp from 'lodash/fp';

export const headerToString = (header?: AuditEventBundleHeader) => {
  return `ID: ${header.agent.agentId}, Username: ${header.agent.agentUsername} \-> ID: ${header.target.targetId}`;
};

export const propertyChangeToString = (propChange: AuditTargetPropertyChange) => {
  const MISSING_VALUE = 'n/a';
  return `${propChange.targetProperty}: ${propChange.previousValue || MISSING_VALUE} \-> ${propChange.newValue || MISSING_VALUE}`;
};

export const eventBundleToString = (eventBundle: AuditEventBundle) => {
  const propertyChanges = fp.map(propertyChangeToString)(eventBundle.propertyChanges);
  return `${headerToString(eventBundle.header)}\n\t# Prop Changes: ${propertyChanges.join('\n')}`;
};

export const actionToString = (action: AuditAction) => {
  return fp.map(eventBundleToString)(action.eventBundles).join('\n');
};
