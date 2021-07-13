import { Browser, ConsoleMessage, launch, LaunchOptions, Page, Request } from 'puppeteer';
import {
  showFailedResponse,
  describeJsHandle,
  getRequestData,
  isLoggable,
  logError,
  showResponse,
  transformResponseBody
} from './page-events-helper';
import { logger } from './logger';
import { defaultLaunchOptions } from './page-options';
import fs from 'fs-extra';
import { signIn, signInWithAccessToken } from 'utils/test-utils';
import { savePageToFile, takeScreenshot } from 'utils/save-file-utils';

const failScreenshotDir = 'logs/screenshot';
const failHtmlDir = 'logs/html';
const { CIRCLE_BUILD_NUM } = process.env;
const userAgent =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) ' +
  'Chrome/80.0.3987.149 Safari/537.36' +
  (CIRCLE_BUILD_NUM ? ` (circle-build-number/${CIRCLE_BUILD_NUM})` : '');

/**
 * Launch new Chrome.
 * @param launchOpts: {@link LaunchOptions} New browser launch options.
 * @return {@link Browser}
 */
export const launchBrowser = async (launchOpts?: LaunchOptions): Promise<Browser> => {
  return launch(launchOpts ? launchOpts : defaultLaunchOptions);
};

/**
 * Launch new incognito page.
 * @param browser: {@link Browser}
 * @return Page
 */
export const launchPage = async (browser: Browser): Promise<Page> => {
  const incognitoPage = await browser.createIncognitoBrowserContext().then((context) => context.newPage());
  await initPageBeforeTest(incognitoPage);
  return incognitoPage;
};

export const withBrowser = (launchOpts?: LaunchOptions) => async (
  testFn: (browser: Browser) => Promise<void>
): Promise<void> => {
  const browser = await launchBrowser(launchOpts);
  try {
    await testFn(browser);
  } finally {
    await browser
      .close()
      .then(() => logger.info('Closing browser'))
      .catch((err: Error) => {
        console.error(`Unable to close browser. Error message: ${err.message}`);
      });
  }
};

/**
 * Launch new browser and incognito page. Opens Workbench Login page.
 * @param launchOpts: {@link LaunchOptions} New browser launch options.
 */
export const withPage = (launchOpts?: LaunchOptions) => async (
  testFn: (page: Page, browser: Browser) => Promise<void>
): Promise<void> => {
  await withBrowser(launchOpts)(async (browser) => {
    const incognitoPage = await browser.createIncognitoBrowserContext().then((context) => context.newPage());
    try {
      await initPageBeforeTest(incognitoPage);
      await testFn(incognitoPage, browser);
    } catch (err) {
      if (err instanceof Error) {
        logger.error(err.message);
      }
      // Take screenshot and save html contents immediately after failure.
      await fs.ensureDir(failScreenshotDir);
      await fs.ensureDir(failHtmlDir);
      await takeScreenshot(incognitoPage, `${__SPEC_NAME__}.png`);
      await savePageToFile(incognitoPage, `${__SPEC_NAME__}.html`);
      throw err;
    } finally {
      await incognitoPage
        .close()
        .then(() => logger.info('Closing page'))
        .catch((err: Error) => {
          console.error(`Unable to close page. Error message: ${err.message}`);
        });
    }
  });
};

export const withSignIn = (opts: { userEmail?: string; password?: string } = {}) => async (
  testFn: (page: Page, browser: Browser) => Promise<void>
): Promise<void> => {
  const { userEmail, password } = opts;
  await withPage()(async (page, browser) => {
    userEmail ? await signIn(page, userEmail, password) : await signInWithAccessToken(page);
    await testFn(page, browser);
  });
};

const getPageTitle = async (page: Page) => {
  return await page
    .$eval('title', (title) => {
      return title.textContent;
    })
    .catch(() => {
      return '';
    });
};

/**
 * Set up page common properties:
 * - Page view port
 * - Page user-agent
 * - Page navigation timeout
 * - waitFor functions timeout
 */
export const initPageBeforeTest = async (page: Page): Promise<void> => {
  page.setDefaultNavigationTimeout(90000); // Puppeteer default timeout is 30 seconds.
  await page.setUserAgent(userAgent);
  await page.setViewport({ width: 1300, height: 0 });
  await page.setRequestInterception(true);

  /**
   * Emitted when a page issues a request. The request object is read-only.
   * In order to intercept and mutate requests, see page.setRequestInterceptionEnabled.
   */
  page.on('request', (request: Request) => {
    if (isLoggable(request)) {
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
    if (showFailedResponse(request)) {
      await logError(request);
    }
  });

  /** Emitted when a request finishes. */
  page.on('requestfinished', async (request: Request) => {
    let method;
    let url;
    let status;
    try {
      method = request.method();
      const resp = request.response();
      url = resp.url();
      status = resp.status();
      if (request.failure() != null || !resp.ok()) {
        await logError(request);
      } else {
        if (isLoggable(request)) {
          let text = `Request finished: ${status} ${method} ${url}`;
          if (request.method() !== 'OPTIONS' && showResponse(request)) {
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
    if (message.args().length === 0) return;
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
    logger.error(`PAGE ERROR: "${title}"\n${error.message}`);
  });

  /** Emitted when an uncaught exception happens within the page. */
  page.on('pageerror', async (error: Error) => {
    const title = await getPageTitle(page);
    logger.error(`PAGEERROR: "${title}"\n${error.message}`);
  });
};
