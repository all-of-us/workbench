import { ErrorResponse } from 'generated/fetch';

import { defaultErrorResponseFormatter } from './errors';

describe('defaultErrorResponseFormatter', () => {
  test.each([
    ['the error response has no details', {}, 'An API error occurred.'],
    [
      'an HTTP status code is present',
      { statusCode: 404 },
      'An API error occurred with HTTP status code 404.',
    ],
    [
      'a unique ID is present',
      { errorUniqueId: 'abcdef' },
      'An API error occurred with unique ID abcdef.',
    ],
    [
      'an HTTP status code and a unique ID are present',
      { statusCode: 404, errorUniqueId: 'abcdef' },
      'An API error occurred with HTTP status code 404 and unique ID abcdef.',
    ],
    [
      'a message is present',
      { message: 'You do not have access to workspace my-test-data' },
      'An API error occurred: You do not have access to workspace my-test-data',
    ],
    [
      'an HTTP status code, a unique ID, and a message are all present',
      {
        statusCode: 404,
        errorUniqueId: 'abcdef',
        message: 'You do not have access to workspace my-test-data',
      },
      'An API error occurred with HTTP status code 404 and unique ID abcdef: You do not have access to workspace my-test-data',
    ],
  ])(
    'Should return the expected result when %s',
    (desc, errorResponse: ErrorResponse, expected: string) => {
      expect(defaultErrorResponseFormatter(errorResponse)).toBe(expected);
    }
  );
});
