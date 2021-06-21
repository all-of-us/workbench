import { Page } from 'puppeteer';
import fs from 'fs-extra';

export const takeScreenshot = async (page: Page, filePath: string) => {
  await page.screenshot({ path: filePath, fullPage: true });
  console.info(`Saved screenshot: ${filePath}`);
};

export const savePageToFile = async (page: Page, htmlFile: string) => {
  const htmlContent = await page.content();
  return new Promise((resolve, reject) => {
    fs.writeFile(htmlFile, htmlContent, 'utf8', (error) => {
      if (error) {
        console.error('Failed to save html file. ' + error);
        reject(false);
      } else {
        console.info('Saved html file: ' + htmlFile);
        resolve(true);
      }
    });
  });
};

export const savePdf = async (page: Page, pdfFile: string) => {
  await page.pdf({ format: 'A4', path: pdfFile });
};
