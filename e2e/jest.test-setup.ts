import { logger } from 'libs/logger';
import * as fp from 'lodash/fp';
import { ConsoleMessage, JSHandle, Request } from 'puppeteer';

const userAgent =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko)' +
  ' Chrome/80.0.3987.149 Safari/537.36';

/**
 * Set up page common properties:
 * - Page view port
 * - Page user-agent
 * - Page navigation timeout
 * - waitFor functions timeout
 */
beforeEach(async () => {
  await jestPuppeteer.resetPage();
  await jestPuppeteer.resetBrowser();
  await page.setUserAgent(userAgent);
  await page.setViewport({ width: 1300, height: 0 });
  page.setDefaultNavigationTimeout(60000); // Puppeteer default timeout is 30 seconds.
  await page.setRequestInterception(true);

  /**
   * Emitted when a page issues a request. The request object is read-only.
   * In order to intercept and mutate requests, see page.setRequestInterceptionEnabled.
   */
  page.on('request', (request: Request) => {
    if (isWorkbenchRequest(request)) {
      const requestBody = getRequestData(request);
      const body = requestBody.length === 0 ? '' : `\n${requestBody}`;
      logger.log('info', 'Request issued: %s %s %s', request.method(), request.url(), body);
    }
    /**
     * May encounter "Error: Request is already handled!"
     * Workaround: https://github.com/puppeteer/puppeteer/issues/3853#issuecomment-458193921
     */
    return Promise.resolve()
      .then(() => request.continue())
      .catch(() => {
        // Ignored
      });
  });

  // Emitted when a request fails: 4xx..5xx status codes
  page.on('requestfailed', async (request) => {
    if (canLogResponse(request)) {
      await logError(request);
    }
  });

  /** Emitted when a request finishes. */
  page.on('requestfinished', async (request: Request) => {
    let method;
    let url;
    let status;
    try {
      if (canLogResponse(request)) {
        // Save data for log in catch block when exception is thrown.
        method = request.method();
        const resp = request.response();
        url = resp.url();
        status = resp.status();
        if (isApiFailure(request)) {
          await logError(request);
        } else {
          let text = `Request finished: ${status} ${method} ${url}`;
          if (!shouldSkipApiResponseBody(request)) {
            text = `${text}\n${await transformResponseBody(request)}`;
          }
          logger.log('info', text);
        }
      }
    } catch (err) {
      // Try find out what the request was
      logger.log('error', '%s %s %s\n%s', status, method, url, err);
    }
    /**
     * May encounter "Error: Request is already handled!"
     * Workaround: https://github.com/puppeteer/puppeteer/issues/3853#issuecomment-458193921
     */
    return Promise.resolve()
      .then(() => request.continue())
      .catch(() => {
        // Ignored
      });
  });

  /**
   * Emitted when JavaScript within the page calls one of console API methods, e.g. console.log.
   * Also emitted if the page throws an error or a warning.
   */
  page.on('console', async (message: ConsoleMessage) => {
    if (!message.args().length) return;
    const title = await getPageTitle();
    try {
      const args = await Promise.all(message.args().map((jsHandle) => describeJsHandle(jsHandle)));
      const concatenatedText = args.filter((arg) => !!arg).join('\n');
      const msgType = message.type() === 'warning' ? 'warn' : message.type();
      logger.info(`Page Console ${msgType.toUpperCase()}: "${title}"\n${concatenatedText}`);
    } catch (err) {
      console.error(`❗ "${title}"\nException occurred when getting page console message.\n${err}`);
    }
  });

  /** Emitted when the page crashes. */
  page.on('error', async (error: Error) => {
    const title = await getPageTitle();
    try {
      logger.error(`PAGE ERROR: "${title}"\n${error}`);
    } catch (err) {
      console.error(`❗ "${title}"\nException occurred when getting page error.\n${err}`);
    }
  });

  /** Emitted when an uncaught exception happens within the page. */
  page.on('pageerror', async (error: Error) => {
    const title = await getPageTitle();
    try {
      logger.error(`PAGEERROR: "${title}"\n${error}`);
    } catch (err) {
      console.error(`❗ "${title}"\nPage exception occurred when getting pageerror.\n${err}`);
    }
  });
});

afterEach(async () => {
  await page.setRequestInterception(false);
});

const getPageTitle = async () => {
  return await page
    .$eval('title', (title) => {
      return title.textContent;
    })
    .catch(() => {
      return 'getPageTitle() func failed';
    });
};

const describeJsHandle = async (jsHandle: JSHandle): Promise<string> => {
  return jsHandle.executionContext().evaluate((obj) => {
    return obj;
  }, jsHandle);
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

const includeUrl = (request: Request): Request | null => {
  const filters = ['google', 'content-security-index-report', 'analytics', 'status-alert', 'publicDetails', '.js'];
  return !filters.some((urlPart) => request && request.url().includes(urlPart)) ? request : null;
};

const includeResourceType = (request: Request): Request | null => {
  const filters = ['xhr', 'fetch', 'websocket'];
  return filters.some((resource) => request && request.resourceType().includes(resource)) ? request : null;
};

const isWorkbenchApi = (request: Request): Request | null => {
  return request && request.url().match('all-of-us-workbench-(test|staging).appspot.com') != null ? request : null;
};

// Disable logging of API response body in test log to make log less cluttered.
// Following API response body are not helpful for error troubleshooting.
const shouldSkipApiResponseBody = (request: Request): boolean => {
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
  return filters.some((partialUrl) => request && request.url().includes(partialUrl));
};

// Truncate long Api response.
const shouldTruncateResponse = (request: Request): boolean => {
  return request && request.url().endsWith('/v1/workspaces');
};

const isApiFailure = (request: Request): boolean => {
  return (request && request.failure() != null) || !request.response().ok();
};

const notOptionsRequest = (request: Request): Request | null => {
  return request && request.method() !== 'OPTIONS' ? request : null;
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
    return (await request.response().buffer()).toString();
  }
};

const logError = async (request: Request): Promise<void> => {
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

const transformResponseBody = async (request: Request): Promise<string> => {
  if (request) {
    let responseText = stringifyData(await getResponseText(request));
    if (shouldTruncateResponse(request)) {
      // truncate long response. get first two workspace details.
      responseText = fp.isEmpty(JSON.parse(responseText).items)
        ? responseText
        : `truncated...\n${JSON.stringify(JSON.parse(responseText).items.slice(0, 1), null, 2)}`;
    }
    return responseText;
  }
};

const isWorkbenchRequest = fp.flow(isWorkbenchApi, notOptionsRequest, includeResourceType, includeUrl);
const getRequestData = fp.flow(getRequestPostData, stringifyData);
const canLogResponse = fp.flow(isWorkbenchRequest, notRedirectRequest);

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/puppeteer/index.d.ts

afterEach(async () => {
  await page.setRequestInterception(false);
});
