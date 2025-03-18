import { firstPartyCookiesEnabled } from './cookies';

export const DISMISSED_MESSAGES_KEY = 'dismissed-messages';

export const getDismissedMessageIds = (): string[] => {
  if (!firstPartyCookiesEnabled()) {
    return [];
  }
  try {
    const stored = localStorage.getItem(DISMISSED_MESSAGES_KEY);
    return stored ? JSON.parse(stored) : [];
  } catch {
    return [];
  }
};

export const isDismissed = (messageId: string): boolean => {
  if (!firstPartyCookiesEnabled()) {
    return false;
  }
  return getDismissedMessageIds().includes(messageId);
};

export const saveDismissedMessage = (messageId: string): void => {
  if (!firstPartyCookiesEnabled()) {
    return;
  }
  try {
    const dismissedIds = getDismissedMessageIds();
    if (!dismissedIds.includes(messageId)) {
      dismissedIds.push(messageId);
      localStorage.setItem(
        DISMISSED_MESSAGES_KEY,
        JSON.stringify(dismissedIds)
      );
    }
  } catch (e) {
    console.error('Error saving dismissed message', e);
  }
};
