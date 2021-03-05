import { logger } from 'libs/logger';
import * as fp from 'lodash/fp';
import {Request} from 'puppeteer';
const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36';

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
  await page.setViewport({width: 1300, height: 0});
  page.setDefaultNavigationTimeout(60000); // Puppeteer default timeout is 30 seconds.
  await page.setRequestInterception(true);

  const getTitle = async () => {
    return await page.$eval('title', title => {
      return title.textContent;
    }).catch(() =>  {return 'getTitle() func failed'});
  }

  const describeJsHandle = async (jsHandle)  => {
    return jsHandle.executionContext().evaluate(obj => {
      return obj.toString();
    }, jsHandle);
  }

  const stringifyData = (data: string): string => {
    if (!data) return '';
    try {
      return JSON.stringify(JSON.parse(data), null, 2);
    } catch (err)  {
      // If data is not json
      return data;
    }
  }

  const includeUrl = (request: Request): Request | null => {
    const filters = [
      'google',
      'content-security-index-report',
      'analytics',
      'status-alert',
      'publicDetails',
      '.js'
    ];
    return !filters.some((urlPart) => request && request.url().includes(urlPart))
       ? request
       : null;
  }

  const includeResourceType = (request: Request): Request | null => {
    const filters = [
      'xhr',
      'fetch',
      'websocket'
    ];
    return filters.some((resource) => request && request.resourceType().includes(resource))
       ? request
       : null;
  }

  const isWorkbenchApi = (request: Request): Request | null => {
    return request && request.url().match('all-of-us-workbench-(test|staging).appspot.com') != null
       ? request
       : null;
  }

  // Api response won't be logged.
  const shouldSkipApiResponseBody = (request: Request): boolean => {
    const filters = [
      '/readonly',
      '/chartinfo/',
      'page-visits',
      '/generateCode/',
      '/criteria/CONDITION/search/',
      '/criteria/',
      '/cdrVersions',
      '/config',
      '/user-recent-workspaces',
      '/profile'
    ];
    return filters.some((partialUrl) => request && request.url().includes(partialUrl));
  }

  // Truncate long Api response.
  const shouldTruncateResponse = (request: Request): boolean => {
    return request && (request.url().endsWith('/v1/workspaces'));
  }

  const isApiFailure = (request: Request): boolean => {
    return request && request.failure() != null || !request.response().ok();
  }

  const notOptionsRequest = (request: Request): Request | null => {
    return request && request.method() !== 'OPTIONS' ? request : null;
  }

  const getRequestPostData = (request: Request): string | undefined => {
    return request && request.postData() ? request.postData() : undefined;
  }

  const notRedirectRequest = (request: Request): boolean => {
    // Response body can only be accessed for non-redirect requests
    return request && request.redirectChain().length === 0 && !request.isNavigationRequest();
  }

  const getResponseText = async (request: Request): Promise<string> => {
    return (await (request.response()).buffer()).toString();
  }

  const logError = async (request: Request): Promise<void> => {
    const response = request.response();
    const failureText = request.failure() !== null ? stringifyData(request.failure().errorText) : '';
    const responseText = stringifyData(await getResponseText(request));
    logger.error('Request failed: '
       + `${response.status()} ${request.method()} ${request.url()}\n${responseText}\n${failureText}`);
  }

  const transformResponseBody = async (request: Request): Promise<string> => {
    if (request) {
      let responseText = stringifyData(await getResponseText(request));
      if (shouldTruncateResponse(request)) {
        // truncate long response. get first two workspace details.
        responseText = fp.isEmpty(JSON.parse(responseText).items)
           ? responseText
           : `truncated...\n${JSON.stringify(JSON.parse(responseText).items.slice(0, 2), null, 2)}`;
      }
      return responseText;
    }
  }

  const isWorkbenchRequest = fp.flow(isWorkbenchApi, notOptionsRequest, includeResourceType, includeUrl);
  const getRequestData = fp.flow(getRequestPostData, stringifyData);
  const canLogResponse = fp.flow(isWorkbenchRequest, notRedirectRequest);

  // New request initiated
  page.on('request', (request) => {
    if (isWorkbenchRequest(request)) {
      const requestBody = getRequestData(request);
      const body = requestBody.length === 0 ? '' : `\n${requestBody}`;
      logger.debug(`Request issued: ${request.method()} ${request.url()}` + body);
    }
    try {
      request.continue();
      // tslint:disable-next-line:no-empty
    } catch (e) {
    }
  });

  page.on('requestfinished', async (request) => {
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
          if (shouldSkipApiResponseBody(request)) {
            logger.info(`Request finished: ${status} ${method} ${url}`);
          } else {
            logger.info('Request finished: ' +
               `${status} ${method} ${url}\n${await transformResponseBody(request)}`);
          }
        }
      }
    } catch (err) {
      // Try find out what the request was
      logger.error(`${err}\n${status} ${method} ${url}`);
    }
    try {
      await request.continue();
    } catch (e) {
      // Ignored
    }
  });

  page.on('console', async (message) => {
    if (!message.args().length) {
      return;
    }
    const title = await getTitle();
    try {
      const args = await Promise.all(message.args().map(a => describeJsHandle(a)));
      switch (message.type()) {
        case "warning":
          logger.warn(`${title}\n`, ...args);
          break;
        case "error":
          logger.error(`${title}\n`, ...args);
          break;
        default:
          logger.info(`${title}\n`, ...args);
      }
    } catch (err) {
      logger.error(`${title}\nException occurred when getting Console message.\n${err}\n${message.text()}`);
    }
  });

  page.on('error', async (error) => {
    const title = await getTitle();
    try {
      logger.error(`${title}\nError message: ${error.message}\nStack: ${error.stack}`);
    } catch (err) {
      logger.error(`${title}\nException occurred when getting error.\n${err}`);
    }
  });

  page.on('pageerror', async (error) => {
    const title = await getTitle();
    try {
      logger.error(`${title}\nPage error message: ${error.message}\nStack: ${error.stack}`);
    } catch (err) {
      logger.error(`${title}\nPage exception occurred when getting pageerror.\n${err}`);
    }
  })

});

afterEach(async () => {
  await page.setRequestInterception(false);
});
