import * as cookies from './cookies';
import {
  getDismissedMessageIds,
  isDismissed,
  saveDismissedMessage,
} from './dismissed-messages';

// Mock the cookies module
jest.mock('./cookies', () => ({
  firstPartyCookiesEnabled: jest.fn(),
}));

describe('dismissed-messages', () => {
  let localStorageMock: { [key: string]: string };
  const DISMISSED_MESSAGES_KEY = 'dismissed-messages';

  beforeEach(() => {
    // Clear all mocks
    jest.clearAllMocks();

    // Mock localStorage getItem, setItem, and clear
    localStorageMock = {};
    Storage.prototype.getItem = jest.fn(
      (key: string) => localStorageMock[key] || null
    );
    Storage.prototype.setItem = jest.fn((key: string, value: string) => {
      localStorageMock[key] = value;
    });
  });

  describe('getDismissedMessageIds', () => {
    it('returns an empty array when cookies are disabled', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(false);

      const result = getDismissedMessageIds();

      expect(result).toEqual([]);
      expect(localStorage.getItem).not.toHaveBeenCalled();
    });

    it('returns an empty array when localStorage is empty', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);

      const result = getDismissedMessageIds();

      expect(result).toEqual([]);
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });

    it('parses and returns dismissed message IDs when available', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);
      const messageIds = ['message-1', 'message-2'];
      localStorageMock[DISMISSED_MESSAGES_KEY] = JSON.stringify(messageIds);

      const result = getDismissedMessageIds();

      expect(result).toEqual(messageIds);
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });

    it('returns an empty array when localStorage throws an error', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);
      (localStorage.getItem as jest.Mock).mockImplementation(() => {
        throw new Error('Storage error');
      });

      const result = getDismissedMessageIds();

      expect(result).toEqual([]);
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });

    it('returns an empty array when JSON parsing fails', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);
      localStorageMock[DISMISSED_MESSAGES_KEY] = 'invalid-json';

      const result = getDismissedMessageIds();

      expect(result).toEqual([]);
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });
  });

  describe('isDismissed', () => {
    it('returns false when cookies are disabled', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(false);

      const result = isDismissed('test-message');

      expect(result).toBe(false);
      expect(localStorage.getItem).not.toHaveBeenCalled();
    });

    it('returns false when the message ID is not in localStorage', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);
      localStorageMock[DISMISSED_MESSAGES_KEY] = JSON.stringify([
        'other-message',
      ]);

      const result = isDismissed('test-message');

      expect(result).toBe(false);
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });

    it('returns true when the message ID is in localStorage', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);
      localStorageMock[DISMISSED_MESSAGES_KEY] = JSON.stringify([
        'test-message',
        'other-message',
      ]);

      const result = isDismissed('test-message');

      expect(result).toBe(true);
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });
  });

  describe('saveDismissedMessage', () => {
    it('does nothing when cookies are disabled', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(false);

      saveDismissedMessage('test-message');

      expect(localStorage.getItem).not.toHaveBeenCalled();
      expect(localStorage.setItem).not.toHaveBeenCalled();
    });

    it('adds message ID to localStorage when it does not exist', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);
      localStorageMock[DISMISSED_MESSAGES_KEY] = JSON.stringify([
        'existing-message',
      ]);

      saveDismissedMessage('test-message');

      const expectedIds = ['existing-message', 'test-message'];
      expect(localStorage.setItem).toHaveBeenCalledWith(
        DISMISSED_MESSAGES_KEY,
        JSON.stringify(expectedIds)
      );
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });

    it('does not add duplicate message IDs', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);
      localStorageMock[DISMISSED_MESSAGES_KEY] = JSON.stringify([
        'test-message',
        'other-message',
      ]);

      saveDismissedMessage('test-message');

      // Should not have called setItem since the message is already in the list
      expect(localStorage.setItem).not.toHaveBeenCalled();
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });

    it('creates a new list when localStorage is empty', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);

      saveDismissedMessage('test-message');

      expect(localStorage.setItem).toHaveBeenCalledWith(
        DISMISSED_MESSAGES_KEY,
        JSON.stringify(['test-message'])
      );
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });

    it('handles errors gracefully', () => {
      (cookies.firstPartyCookiesEnabled as jest.Mock).mockReturnValue(true);
      (localStorage.getItem as jest.Mock).mockImplementation(() => {
        throw new Error('Storage error');
      });

      // This should not throw an error
      saveDismissedMessage('test-message');

      // The function should handle the error internally
      expect(localStorage.getItem).toHaveBeenCalledWith(DISMISSED_MESSAGES_KEY);
    });
  });
});
