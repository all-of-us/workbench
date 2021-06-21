import fp from 'lodash/fp';
import fs from 'fs-extra';
import { logger } from 'libs/logger';
import { Browser, ConsoleMessage, defaultArgs, JSHandle, launch, LaunchOptions, Page, Request } from 'puppeteer';
import { savePageToFile, takeScreenshot } from './test-error-manager';
import GoogleLoginPage from 'app/page/google-login';

const { PUPPETEER_DEBUG, PUPPETEER_HEADLESS, CI, CIRCLE_BUILD_NUM } = process.env;
const failScreenshotDir = 'logs/screenshot';
const failHtmlDir = 'logs/html';
const isDebug = PUPPETEER_DEBUG === 'true';
const isHeadless = isDebug ? false : CI === 'true' || (PUPPETEER_HEADLESS || 'true') === 'true';
const slowMotion = 20;
const isCi = CI === 'true';
const userAgent =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko)' +
  ' Chrome/80.0.3987.149 Safari/537.36' +
  (CIRCLE_BUILD_NUM ? ` (circle-build-number/${CIRCLE_BUILD_NUM})` : '');

const customChromeOptions = [
  // Reduce cpu and memory usage. Disables one-site-per-process security policy, dedicated processes for site origins.
  '--disable-features=BlinkGenPropertyTrees,IsolateOrigins,site-per-process',
  // Page not loading Login URL when launched Chromium without this flag. It disables use of zygote process for forking child processes.
  // https://codereview.chromium.org/2384163002
  '--no-zygote',
  '--no-sandbox', // required for --no-zygote flag
  '--safebrowsing-disable-auto-update',
  '--window-size=1300,1024'
];

// Append to Puppeteer default chrome flags.
// https://github.com/puppeteer/puppeteer/blob/33f1967072e07824c5bf6a8c1336f844d9efaabf/lib/Launcher.js#L261
const defaultChromeOptions = defaultArgs({
  userDataDir: null,
  headless: isHeadless,
  devtools: true,
  args: customChromeOptions
});

// Need for running Puppeteer headless Chromium in docker container.
const ciChromeOptions = fp.concat(defaultChromeOptions, [
  '--disable-gpu', // https://bugs.chromium.org/p/chromium/issues/detail?id=737678#c10
  '--disable-setuid-sandbox'
]);

const defaultLaunchOptions = {
  devtools: isDebug,
  slowMo: slowMotion,
  defaultViewport: null,
  ignoreDefaultArgs: true,
  ignoreHTTPSErrors: true,
  args: isCi ? ciChromeOptions : defaultChromeOptions
};

const setupPageBeforeEachTest = async (page: Page): Promise<void> => {
  page.setDefaultNavigationTimeout(90000); // Puppeteer default timeout is 30 seconds.
  await page.setUserAgent(userAgent);
  await page.setViewport({ width: 1300, height: 0 });
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
   * https://github.com/puppeteer/puppeteer/issues/3397#issuecomment-429325514
   */
  page.on('console', async (message: ConsoleMessage) => {
    if ((await message.args()).length === 0) return;
    const title = await getPageTitle(page);
    try {
      await Promise.all(message.args().map((arg) => arg.jsonValue())).then((values) => {
        const allMessages = values
          .filter((value) => {
            if (JSON.stringify(value) === JSON.stringify({})) {
              // eslint-disable-next-line @typescript-eslint/ban-ts-comment
              // @ts-ignore
              const { subtype, description } = arg._remoteObject;
              return `${subtype ? subtype.toUpperCase() : ''}:\n${description}`;
            }
            return values;
          })
          .join('\n');
        if (allMessages.trim().length > 0) {
          logger.info(`Page Console: ${title}\n${allMessages}`);
        }
      });
    } catch (ex) {
      // arg.jsonValue() sometimes throws exception. Try another way when encountering error.
      await Promise.all(message.args().map((jsHandle) => describeJsHandle(jsHandle)))
        .then((args) => {
          const allMessages = args.filter((arg) => !!arg).join('\n');
          const msgType = message.type() === 'warning' ? 'warn' : message.type();
          logger.info(`Page Console ${msgType.toUpperCase()}: "${title}"\n${allMessages}`);
        })
        .catch((ex1) => {
          logger.error(`Exception thrown when reading page console: ${ex1}`);
        });
    }
  });

  /** Emitted when the page crashes. */
  page.on('error', async (error: Error) => {
    const title = await getPageTitle(page);
    try {
      logger.error(`PAGE ERROR: "${title}"\n${error}`);
    } catch (err) {
      console.error(`❗ Exception when getting page error.\n${err}`);
    }
  });

  /** Emitted when an uncaught exception happens within the page. */
  page.on('pageerror', async (error: Error) => {
    const title = await getPageTitle(page);
    try {
      logger.error(`PAGEERROR: "${title}"\n${error}`);
    } catch (err) {
      console.error(`❗ Exception when getting pageerror.\n${err}`);
    }
  });
};

/**
 *
 * @param launchOpts: {@link LaunchOptions} New browser launch options.
 */
export const withBrowser = (launchOpts?: LaunchOptions) => async (
  testFn: (browser: Browser) => Promise<void>
): Promise<void> => {
  console.log(`PUPPETEER_DEBUG: ${PUPPETEER_DEBUG}`);
  console.log(`PUPPETEER_HEADLESS: ${PUPPETEER_HEADLESS}`);
  console.log(`isDebug: ${isDebug}`);

  const browser = await launch(launchOpts ? launchOpts : defaultLaunchOptions);
  try {
    console.log('withBrowser begin');
    return await testFn(browser);
  } catch (err) {
    if (err instanceof Error) {
      logger.error(err.message);
    }
    throw err;
  } finally {
    await browser.close().catch((err) => {
      console.error(`Unable to close page. Error message: ${err.message}`);
    });
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
};

/**
 * Launch new browser and incognito page. Opens Workbench Login page.
 * @param launchOpts: {@link LaunchOptions} New browser launch options.
 */
export const withPage = (launchOpts?: LaunchOptions) => async (
  testFn: (page: Page, browser: Browser) => Promise<void>
): Promise<void> => {
  let incognitoPage;
  await withBrowser(launchOpts)(async (browser) => {
    try {
      console.log('withPage begin');
      incognitoPage = await browser.createIncognitoBrowserContext().then((context) => context.newPage());
      await setupPageBeforeEachTest(incognitoPage);
      await new GoogleLoginPage(incognitoPage).load();
      return await testFn(incognitoPage, browser);
    } catch (err) {
      if (err instanceof Error) {
        logger.error(err.message);
      }
      await fs.ensureDir(failScreenshotDir);
      await fs.ensureDir(failHtmlDir);
      const testName = testNames[testNames.length - 1];
      const screenshotFile = `${failScreenshotDir}/${testName}.png`;
      const htmlFile = `${failHtmlDir}/${testName}.html`;
      await takeScreenshot(incognitoPage, screenshotFile);
      await savePageToFile(incognitoPage, htmlFile);
      throw err;
    } finally {
      await incognitoPage.close().catch((err) => {
        console.error(`Unable to close page. Error message: ${err.message}`);
      });
      await new Promise((resolve) => setTimeout(resolve, 1000));
    }
  });
};

/*
export const withPage = (browser: Browser) => async (
  test,
  opts: { signInWithToken?: boolean; userEmail?: string; pwd?: string } = {}
): Promise<unknown> => {
  const { signInWithToken = true, userEmail, pwd } = opts;
  const page = await browser.createIncognitoBrowserContext().then((context) => context.newPage());
  try {
    console.log('withPage begin');
    await setUpBeforeEachTest(page);
    if (signInWithToken) {
      await signInWithAccessToken(page);
    } else if (userEmail && pwd) {
      await signIn(page, userEmail, pwd);
    }
    return await test(page);
  } catch (err) {
    if (err instanceof Error) {
      logger.error(err.message);
    }
    await fs.ensureDir(failScreenshotDir);
    await fs.ensureDir(failHtmlDir);
    const testName = testNames[testNames.length - 1];
    const screenshotFile = `${failScreenshotDir}/${testName}.png`;
    const htmlFile = `${failHtmlDir}/${testName}.html`;
    await takeScreenshot(page, screenshotFile);
    await savePageToFile(page, htmlFile);
    throw err;
  } finally {
    await page.close().catch((err) => {
      console.error(`ERROR when close page.\n${err}`);
    });
  }
};
*/

/*
export const withAuthenticatedUser = async (authOpts: { userEmail?: string; pwd?: string } = {}): Promise<void> => {
  const { userEmail, pwd } = authOpts;
  const loginFn = async (page) => {
    !!userEmail && !!pwd ? await signIn(page, userEmail, pwd) : await signInWithAccessToken(page);
  };
  await Promise.all(
      fp.flow(
      fp.flow(withBrowser, withPage, loginFn)),
  )
  return page;
};
*/

const getPageTitle = async (page: Page) => {
  return await page
    .$eval('title', (title) => {
      return title.textContent;
    })
    .catch(() => {
      return '';
    });
};

const describeJsHandle = async (jsHandle: JSHandle): Promise<string> => {
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
    try {
      return (await request.response().buffer()).toString();
    } catch (err) {
      console.error(`Puppeteer error during get response text.\n${err}`);
      return undefined;
    }
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
