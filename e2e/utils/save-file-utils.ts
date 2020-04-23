import { ensureDir, writeFile } from 'fs-extra';
import {Page} from 'puppeteer';
import {extractPageName, makeDateTimeStr} from './str-utils';

/**
 * Save Html source to a file. Useful for test failure troubleshooting.
 * @param {Puppeteer.Page} page
 * @param {string} fileName file with path.
 */
export async function savePageToFile(page: Page, fileName?: string): Promise<boolean> {
  const logDir = 'logs/html';
  await ensureDir(logDir);
  const name = fileName || await extractPageName(page);
  const htmlFile = `${logDir}/${name}.html`;
  const htmlContent = await page.content();
  return new Promise((resolve, reject) => {
    writeFile(htmlFile, htmlContent, 'utf8', error => {
      if (error) {
        console.error(`Failed to save file. ` + error);
        reject(false);
      } else {
        console.log('Saved file ' + htmlFile);
        resolve(true);
      }
    })
  });
}

/**
 * Take a full-page screenshot, save file in .png format in logs/screenshot directory.
 * @param fileName
 */
export async function takeScreenshot(page: Page, fileName?: string): Promise<void> {
  const screenshotDir = 'logs/screenshot';
  await ensureDir(screenshotDir);
  const fName = fileName || await makeDateTimeStr(await extractPageName(page));
  const screenshotFile = `${screenshotDir}/${fName}.png`;
  await page.screenshot({path: screenshotFile, fullPage: true});
  console.log('Saved screenshot ' + screenshotFile);
}
