import {
  AuditAction,
  AuditEventBundle,
  AuditEventBundleHeader,
  AuditTargetPropertyChange
} from 'generated/fetch';
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

// based on https://stackoverflow.com/a/44661948. Use the browser's native user-driven file
// download mechanism.
export const downloadTextFile = (fileName, contents) => {
  const file = new Blob([contents], {type: 'text/plain'});
  const element = document.createElement('a');
  element.href = URL.createObjectURL(file);
  element.download = fileName;
  document.body.appendChild(element); // Required for this to work in FireFox
  element.click();
  document.body.removeChild(element);
};

export const usernameWithoutDomain = (username: string) => {
  return username ? username.substring(0, username.indexOf('@')) : '';
};
