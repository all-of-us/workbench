import { ensureDir, writeFile } from 'fs-extra';
import { Page } from 'puppeteer';
import { extractPageName, makeDateTimeStr } from './str-utils';
import { logger } from 'libs/logger';

/**
 * Save Html source to a file. Useful for test failure troubleshooting.
 * @param {Puppeteer.Page} page
 * @param {string} fileName file with path.
 */
export async function savePageToFile(page: Page, fileName?: string): Promise<boolean> {
  const logDir = 'logs/html';
  await ensureDir(logDir);
  const name = fileName || (await extractPageName(page));
  const htmlFile = `${logDir}/${makeDateTimeStr(name)}.html`;
  const htmlContent = await page.content();
  return new Promise((resolve, reject) => {
    writeFile(htmlFile, htmlContent, 'utf8', (error) => {
      if (error) {
        logger.error(`Failed to save file. ` + error);
        reject(false);
      } else {
        logger.info('Saved file ' + htmlFile);
        resolve(true);
      }
    });
  });
}

/**
 * Take a full-page screenshot, save file in .png format in logs/screenshot directory.
 * @param fileName
 */
export async function takeScreenshot(page: Page, fileName?: string): Promise<void> {
  const screenshotDir = 'logs/screenshot';
  await ensureDir(screenshotDir);
  const name = fileName || (await extractPageName(page));
  const screenshotFile = `${screenshotDir}/${makeDateTimeStr(name)}.png`;
  await page.screenshot({ path: screenshotFile, fullPage: true });
  logger.info('Saved screenshot ' + screenshotFile);
}
