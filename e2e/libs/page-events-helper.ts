import fp from 'lodash/fp';
import { JSHandle, Request } from 'puppeteer';
import { logger } from 'libs/logger';

export const describeJsHandle = async (jsHandle: JSHandle): Promise<string> => {
  return jsHandle
    .executionContext()
    .evaluateHandle((obj) => {
      if (obj instanceof Error) {
        return obj.message;
      }
      return obj;
    }, jsHandle)
    .then(async (jsHandle) => {
      return (await jsHandle.jsonValue()) as string;
    });
};

const stringifyData = (data: string): string => {
  if (!data) return '';
  try {
    return JSON.stringify(JSON.parse(data), null, 2);
  } catch (err) {
    // If data is not json
    return data;
  }
};

/**
 * Filter requests to exclude unwanted irrelevant requests.
 * @param request
 * @return: Returns request if this request does not match the exclude pattern.
 *  Returns null if this request matches the exclude pattern.
 */
const isWorkbenchRequest = (request: Request): Request | null => {
  const unwantedRequests = [
    'google',
    'content-security-index-report',
    'analytics',
    'status-alert',
    'publicDetails',
    '.js',
    'favicon.ico'
  ];
  return request &&
    !unwantedRequests.some((urlPart) => request.url().includes(urlPart)) &&
    /all-of-us-workbench-(test|staging).appspot.com/.exec(request.url()) != null
    ? request
    : null;
};

/**
 * Returns this request only if request resource type are: "xhr", "fetch", "websocket", and "other".
 * @param request
 */
const includeXhrResourceType = (request: Request): Request | null => {
  const filters = ['xhr', 'fetch', 'websocket', 'other'];
  return filters.some((resource) => request && request.resourceType().includes(resource)) ? request : null;
};

// Disable logging of API response body in test log to make log less cluttered.
// Following API response body are not helpful for error troubleshooting.
export const shouldLogResponse = (request: Request): boolean => {
  const filters = [
    '/readonly',
    '/chartinfo/',
    '/page-visits',
    '/generateCode/',
    '/criteria/CONDITION/search/',
    '/criteria/',
    '/cdrVersions',
    '/config',
    '/user-recent-workspaces',
    '/user-recent-resources',
    '/profile'
  ];
  return !filters.some((partialUrl) => request && request.url().includes(partialUrl));
};

// Truncate long Api response.
const isResponseTruncatable = (request: Request): boolean => {
  return request && request.url().endsWith('/v1/workspaces');
};

const getRequestPostData = (request: Request): string | undefined => {
  return request && request.postData() ? request.postData() : undefined;
};

const notRedirectRequest = (request: Request): boolean => {
  // Response body can only be accessed for non-redirect requests
  return request && request.redirectChain().length === 0 && !request.isNavigationRequest();
};

const getResponseText = async (request: Request): Promise<string> => {
  const REDIRECT_CODE_START = 300;
  const REDIRECT_CODE_END = 308;
  const NO_CONTENT_RESPONSE_CODE = 204;
  const response = request.response();
  // Log response if response it's not a redirect or no-content
  const status = response && response.status();
  if (
    status &&
    !(status >= REDIRECT_CODE_START && status <= REDIRECT_CODE_END) &&
    status !== NO_CONTENT_RESPONSE_CODE
  ) {
    try {
      return (await request.response().buffer()).toString();
    } catch (err) {
      console.error(`Puppeteer error during get response text.\n${err}`);
      return undefined;
    }
  }
};

export const logRequestError = async (request: Request): Promise<void> => {
  const response = request.response();
  const status = response ? response.status() : '';
  const failureText = request.failure() !== null ? stringifyData(request.failure().errorText) : '';
  const responseText = stringifyData(await getResponseText(request));
  logger.log(
    'error',
    'Request failed: %s %s %s\n%s %s',
    status,
    request.method(),
    request.url(),
    responseText,
    failureText
  );
};

export const formatResponseBody = async (request: Request): Promise<string> => {
  if (request) {
    let responseText = stringifyData(await getResponseText(request));
    if (isResponseTruncatable(request)) {
      // truncate long response. get first two workspace details.
      responseText = fp.isEmpty(JSON.parse(responseText).items)
        ? responseText
        : `truncated...\n${JSON.stringify(JSON.parse(responseText).items.slice(0, 1), null, 2)}`;
    }
    return responseText;
  }
};

export const isLoggable = fp.flow(isWorkbenchRequest, includeXhrResourceType, notRedirectRequest);

export const getRequestData = fp.flow(getRequestPostData, stringifyData);

export const showFailedResponse = fp.flow(isWorkbenchRequest, includeXhrResourceType, notRedirectRequest);
