import { ensureDir, writeFile } from 'fs-extra';
import { Page } from 'puppeteer';

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
        console.error(`Failed to save html file. ERROR: ${error}`);
        reject(false);
      } else {
        console.log(`Saved html file: ${fileName}`);
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
  console.log(`Saved screenshot file: ${fileName}`);
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
  console.log(`Saved pdf file: ${fileName}`);
};
