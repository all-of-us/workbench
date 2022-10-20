import { ErrorCode, ErrorResponse } from 'generated/fetch';

import {
  defaultErrorResponseFormatter,
  errorHandlerWithFallback,
  FALLBACK_ERROR_TITLE,
} from './errors';
import { NotificationStore } from './stores';

describe('defaultErrorResponseFormatter', () => {
  test.each([
    ['the error response has no details', {}, 'An API error occurred.'],
    [
      'the error response could not be parsed, and no status code is present',
      { errorCode: ErrorCode.PARSEERROR },
      'An API error occurred.',
    ],
    [
      'the error response could not be parsed, but a status code is present',
      { errorCode: ErrorCode.PARSEERROR, statusCode: 400 },
      'An API error occurred with HTTP status code 400.',
    ],
    [
      'an error code is present',
      { errorCode: ErrorCode.USERDISABLED },
      'An API error of type USER_DISABLED occurred.',
    ],
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
      'a message is present',
      { message: 'You do not have access to workspace my-test-data' },
      'An API error occurred: You do not have access to workspace my-test-data',
    ],
    [
      'an error code, an HTTP status code, a unique ID, and a message are all present',
      {
        errorCode: ErrorCode.USERDISABLED,
        statusCode: 404,
        errorUniqueId: 'abcdef',
        message: 'You do not have access to workspace my-test-data',
      },
      'An API error of type USER_DISABLED occurred with HTTP status code 404 and unique ID abcdef: ' +
        'You do not have access to workspace my-test-data',
    ],
  ])(
    'Should return the expected result when %s',
    (desc: string, errorResponse: ErrorResponse, expected: string) => {
      expect(defaultErrorResponseFormatter(errorResponse)).toBe(expected);
    }
  );
});

const custom409Response: NotificationStore = {
  title: 'Conflict',
  message: 'Error saving Cohort XYZ',
};

describe('errorHandlerWithFallback', () => {
  test.each([
    [
      'the API response is unparseable and there are no custom handlers',
      'error',
      undefined,
      undefined,
      {
        title: FALLBACK_ERROR_TITLE,
        message: 'An API error occurred.',
      },
    ],
    [
      'the API response is parseable and there are no custom handlers',
      { json: () => ({ message: 'User not found', statusCode: 404 }) },
      undefined,
      undefined,
      {
        title: FALLBACK_ERROR_TITLE,
        message:
          'An API error occurred with HTTP status code 404: User not found',
      },
    ],
    [
      'the API response is parseable and should be treated as a non-error',
      { json: () => ({ message: 'User not found', statusCode: 404 }) },
      // expectedResponseMatcher -> true
      (er: ErrorResponse) => er.statusCode === 404,
      undefined,
      undefined,
    ],
    [
      'the API response is parseable and a custom response handler is applied',
      { json: () => ({ message: 'Conflict', statusCode: 409 }) },
      // expectedResponseMatcher -> false
      (er: ErrorResponse) => er.statusCode === 404,
      // customErrorResponseFormatter -> matches, returns custom409Response
      (er: ErrorResponse) =>
        er.statusCode === 409 ? custom409Response : undefined,
      custom409Response,
    ],
    [
      'the API response is parseable and a custom response handler is not applicable',
      { json: () => ({ message: 'Unknown Error', statusCode: 400 }) },
      // expectedResponseMatcher -> false
      (er: ErrorResponse) => er.statusCode === 404,
      // customErrorResponseFormatter -> does not match, returns undefined
      (er: ErrorResponse) =>
        er.statusCode === 409 ? custom409Response : undefined,
      {
        title: FALLBACK_ERROR_TITLE,
        message:
          'An API error occurred with HTTP status code 400: Unknown Error',
      },
    ],
  ])(
    'Should return the expected result when %s',
    async (
      desc: string,
      apiErrorResponse,
      expectedResponseMatcher: (ErrorResponse) => boolean,
      customErrorResponseFormatter: (ErrorResponse) => NotificationStore,
      expected: NotificationStore
    ) => {
      expect(
        await errorHandlerWithFallback(
          apiErrorResponse,
          expectedResponseMatcher,
          customErrorResponseFormatter
        )
      ).toStrictEqual(expected);
    }
  );
});
