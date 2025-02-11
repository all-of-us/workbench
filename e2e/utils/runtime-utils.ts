import { Page } from 'puppeteer';
import Button from 'app/element/button';
import { exists } from './element-utils';
import { logger } from 'libs/logger';

const Selector = {
  runtimeInitializerCreateButton: '//*[@role="dialog"]//*[text()="Create Environment" and @role="button"]'
};

export async function initializeRuntimeIfModalPresented(page: Page): Promise<void> {
  let initializeButton: Button;
  try {
    initializeButton = new Button(page, Selector.runtimeInitializerCreateButton);
    await initializeButton.waitUntilEnabled();
  } catch (e) {
    // Expected if the runtime already exists.
    return;
  }

  await initializeButton.click();
}

async function isSecuritySuspended(page: Page): Promise<boolean> {
  return exists(page, '//*[@data-test-id="security-suspended-msg"]', { timeout: 10000 });
}

export async function waitForSecuritySuspendedStatus(
  page: Page,
  suspended = true,
  timeOut = 25 * 60 * 1000
): Promise<void> {
  const startTime = Date.now();
  const pollPeriod = 20 * 1000;

  while (true) {
    // temp for debugging
    const reloadStart = Date.now();
    const reloadResult = await page.reload({
      waitUntil: ['load', 'domcontentloaded', 'networkidle0'],
      timeout: 60 * 1000
    });
    const reloadEnd = Date.now();
    logger.info(
      `Reload ${reloadResult.ok() ? 'succeeded' : 'failed'} after ${(reloadEnd - reloadStart) / 1000} seconds`
    );

    if ((await isSecuritySuspended(page)) === suspended) {
      // Success
      break;
    }

    const waited = Date.now() - startTime;
    if (waited > timeOut - pollPeriod) {
      // temp for debugging
      logger.info(`Waited ${waited / 1000} seconds, longer than timeout of ${timeOut / 1000} seconds`);
      throw new Error(`Timed out waiting for egress security suspension status = ${suspended}`);
    }
    logger.info(`Waited for ${(waited / 1000 / 60).toFixed(2)} minutes`);
    await page.waitForTimeout(pollPeriod);
  }
}
