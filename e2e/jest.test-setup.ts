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
    });
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

  // Api "/workspaces" or "/cdrVersions" response can be truncated
  const isWorkspacesApi = (request: Request): boolean => {
    return request && (request.url().endsWith('/v1/workspaces') || request.url().endsWith('/v1/cdrVersions'));
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
    return request && request.redirectChain().length === 0;
  }

  const getResponseText = async (request: Request): Promise<string> => {
    return (await (request.response()).buffer()).toString();
  }

  const logError = async (request: Request): Promise<void> => {
    const response = request.response();
    const failureText = request.failure() !== null ? stringifyData(request.failure().errorText) : '';
    const responseText = stringifyData(await getResponseText(request));
    console.error('❗ Request failed: ' +
       `${response.status()} ${request.method()} ${request.url()}\n${responseText}\n${failureText}`);
  }

  const logResponse = async (request: Request): Promise<void> => {
    let responseText = stringifyData(await getResponseText(request));
    if (isWorkspacesApi(request)) {
      // truncate long response. get first two workspace details.
      responseText = fp.isEmpty(JSON.parse(responseText).items)
         ? responseText
         : 'truncated...\n' + JSON.stringify(JSON.parse(responseText).items.slice(0, 2), null, 2);
    }
    console.debug('❗ Request finished: ' +
       `${request.response().status()} ${request.method()} ${request.url()}\n${responseText}`);
  }

  const isWorkbenchRequest = fp.flow(isWorkbenchApi, notOptionsRequest, includeResourceType, includeUrl);
  const getRequestData = fp.flow(getRequestPostData, stringifyData);
  const canLogResponse = fp.flow(isWorkbenchRequest, notRedirectRequest);

  // New request initiated
  page.on('request', (request) => {
    if (isWorkbenchRequest(request)) {
      console.debug('❗ Request issued: ' +
         `${request.method()} ${request.url()}\n${getRequestData(request)}`);
    }
    try {
      request.continue();
      // tslint:disable-next-line:no-empty
    } catch (e) {
    }
  });

  page.on('requestfinished', async (request) => {
    if (canLogResponse(request)) {
      if (isApiFailure(request)) {
        await logError(request);
      } else {
        await logResponse(request);
      }
    }
    try {
      await request.continue();
      // tslint:disable-next-line:no-empty
    } catch (e) {
    }
  });

  page.on('console', async (message) => {
    if (!message.args().length) {
      return;
    }
    try {
      const title = await getTitle();
      const args = await Promise.all(message.args().map(a => a.jsonValue()));
      console[message.type() === 'warning' ? 'warn' : message.type()](`❗ ${title}\n`, ...args);
      // tslint:disable-next-line:no-empty
    } catch (err) {
      console.log(err);
    }
  });

  page.on('error', async (error) => {
    const title = await getTitle();
    console.error(`❗ ${title}\npage error: ${error}`);
  });

  page.on('pageerror', async (error) => {
    const title = await getTitle();
    try {
      console.error(`❗ ${title}\npage error: ${error}`);
      // tslint:disable-next-line:no-empty
    } catch (err) {
    }
  })

});

afterEach(async () => {
  await page.setRequestInterception(false);
});
