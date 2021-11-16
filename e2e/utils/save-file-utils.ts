import { ensureDir, writeFile } from 'fs-extra';
import { Page } from 'puppeteer';
import { logger } from 'libs/logger';

/**
 * Save Html source to a file in logs/html directory. Useful for test failure troubleshooting.
 * @param {Page} page
 * @param {string} fileName filename only without path. Example: home-page.html
 */
export const savePageToFile = async (page: Page, fileName: string): Promise<boolean> => {
  const dir = 'logs/html';
  await ensureDir(dir);
  const htmlContent = await page.content();
  return new Promise((resolve, reject) => {
    writeFile(`${dir}/${fileName}`, htmlContent, 'utf8', (error) => {
      if (error) {
        logger.error(`Failed to save html file. ERROR: ${error}`);
        reject(false);
      } else {
        logger.info(`Saved html file: ${fileName}`);
        resolve(true);
      }
    });
  });
};

/**
 * Take a full-page screenshot in png format, save file in logs/screenshot directory.
 * @param {Page} page
 * @param {string} fileName filename only without path. Example: home-page.png
 */
export const takeScreenshot = async (page: Page, fileName: string): Promise<void> => {
  const dir = 'logs/screenshot';
  await ensureDir(dir);
  await page.screenshot({ type: 'png', path: `${dir}/${fileName}`, fullPage: true });
  logger.info(`Saved screenshot file: ${fileName}`);
};

/**
 * Save Html source to a pdf file in logs/screenshot directory.
 * @param {Page} page
 * @param {string} fileName filename only without path. Example: home-page.pdf
 */
export const savePageToPdf = async (page: Page, fileName: string): Promise<void> => {
  const dir = 'logs/screenshot';
  await ensureDir(dir);
  await page.pdf({ format: 'A4', path: `${dir}/${fileName}` });
  logger.info(`Saved pdf file: ${fileName}`);
};
