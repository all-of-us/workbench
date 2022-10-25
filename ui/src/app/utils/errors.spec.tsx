import { ErrorCode } from 'generated/fetch';

import {
  defaultErrorResponseFormatter,
  errorHandlerWithFallback,
  FALLBACK_ERROR_TITLE,
} from './errors';
import { NotificationStore } from './stores';

describe('defaultErrorResponseFormatter', () => {
  test.each([
    ['the error response is empty', {}, 'An API error occurred.'],
    [
      'the error response is not in an expected format',
      { foo: 'bar' },
      'An API error occurred.',
    ],
    [
      'an HTTP status code is present',
      { status: 400 } as Response,
      'An API error occurred with HTTP status code 400.',
    ],
    [
      'an error code is present',
      { json: () => ({ errorCode: ErrorCode.USERDISABLED }) },
      'An API error of type USER_DISABLED occurred.',
    ],
    [
      'a unique ID is present',
      { json: () => ({ errorUniqueId: 'abcdef' }) },
      'An API error occurred with unique ID abcdef.',
    ],
    [
      'a message is present',
      {
        json: () => ({
          message: 'You do not have access to workspace my-test-data',
        }),
      },
      'An API error occurred: You do not have access to workspace my-test-data',
    ],
    [
      'an HTTP status code, an error code, a unique ID, and a message are all present',
      {
        status: 404,
        json: () => ({
          errorCode: ErrorCode.USERDISABLED,
          errorUniqueId: 'abcdef',
          message: 'You do not have access to workspace my-test-data',
        }),
      },
      'An API error of type USER_DISABLED occurred with HTTP status code 404 and unique ID abcdef: ' +
        'You do not have access to workspace my-test-data',
    ],
  ])(
    'Should return the expected result when %s',
    async (desc: string, errorResponse: Response, expected: string) => {
      expect(await defaultErrorResponseFormatter(errorResponse)).toBe(expected);
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
      { status: 404, json: () => ({ message: 'User not found' }) },
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
      { status: 404, json: () => ({ message: 'User not found' }) },
      // expectedResponseMatcher -> true
      (er: Response) => er.status === 404,
      undefined,
      undefined,
    ],
    [
      'the API response is parseable and a custom response handler is applied',
      { status: 409, json: () => ({ message: 'Conflict' }) },
      // expectedResponseMatcher -> false
      (er: Response) => er.status === 404,
      // customErrorResponseFormatter -> matches, returns custom409Response
      (er: Response) => (er.status === 409 ? custom409Response : undefined),
      custom409Response,
    ],
    [
      'the API response is parseable and a custom response handler is not applicable',
      { status: 400, json: () => ({ message: 'Unknown Error' }) },
      // expectedResponseMatcher -> false
      (er: Response) => er.status === 404,
      // customErrorResponseFormatter -> does not match, returns undefined
      (er: Response) => (er.status === 409 ? custom409Response : undefined),
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
      expectedResponseMatcher: (Response) => boolean,
      customErrorResponseFormatter: (Response) => NotificationStore,
      expected: NotificationStore
    ) => {
      expect(
        await errorHandlerWithFallback(
          apiErrorResponse as any as Response,
          expectedResponseMatcher,
          customErrorResponseFormatter
        )
      ).toStrictEqual(expected);
    }
  );
});
