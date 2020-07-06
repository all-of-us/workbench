import * as React from 'react';
import {
  AuditAction,
  AuditEventBundle,
  AuditEventBundleHeader,
  AuditTarget,
  AuditTargetPropertyChange
} from '../../generated';

// Type sold separately
// Type sold separately
export const headerToBundleHeadline = (header?: AuditEventBundleHeader) => {
  return `${header.agent.agentType} ${header.actionType} ${header.target.targetType}`;
};

export const headerToString = (header?: AuditEventBundleHeader) => {
  return `ID: ${header.agent.agentId}, Username: ${header.agent.agentUsername} \-> ID: ${header.target.targetId}`;
};

export const eventBundleToString = (eventBundle: AuditEventBundle) => {
  return `${headerToString(eventBundle.header)}\n\t# Prop Changes: ${eventBundle.propertyChanges.map((pc) => propertyChangeToString(pc)).join('\n')}`;
};

export const propertyChangeToString = (propChange: AuditTargetPropertyChange) => {
  const MISSING_VALUE = 'n/a';
  return `${propChange.targetProperty}: ${propChange.previousValue || MISSING_VALUE} \-> ${propChange.newValue || MISSING_VALUE}`;
};

export const actionToString = (action: AuditAction) => {
  const eventBundles = action.eventBundles.map((event) => {
    return eventBundleToString(event);
  });
  return eventBundles.join('\n');
};

