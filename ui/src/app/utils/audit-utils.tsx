import {
  AuditAction,
  AuditAgent,
  AuditEventBundle,
  AuditEventBundleHeader,
  AuditTarget, AuditTargetPropertyChange
} from '../../generated';
import * as React from 'react';

export const agentToString = (agent?: AuditAgent) => {
  return `${agent.agentType} DB ID: ${agent.agentId} Username: ${agent.agentUsername}`;
};

export const targetToString = (target?: AuditTarget) => {
  return `${target.targetType} DB ID: ${target.targetId}`;
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

