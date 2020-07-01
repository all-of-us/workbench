import {
  AuditAction,
  AuditAgent,
  AuditEventBundle,
  AuditEventBundleHeader,
  AuditTarget, AuditTargetPropertyChange
} from '../../generated';
import * as React from 'react';

// Type sold separately
export const agentToString = (agent?: AuditAgent) => {
  return `ID: ${agent.agentId}, Username: ${agent.agentUsername}`;
};

// Type sold separately
export const targetToString = (target?: AuditTarget) => {
  return `ID: ${target.targetId}`;
};

export const headerToString = (header?: AuditEventBundleHeader) => {
  return `${agentToString(header.agent)} \-> ${targetToString(header.target)}`;
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

