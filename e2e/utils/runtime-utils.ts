import { Page } from 'puppeteer';
import Button from 'app/element/button';

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
  try {
    await page.waitForXPath('//*[@data-test-id="security-suspended-msg"]', { visible: true });
  } catch (e) {
    return false;
  }
  return true;
}

export async function waitForSecuritySuspendedStatus(
  page: Page,
  suspended = true,
  timeOut = 25 * 60 * 1000
): Promise<void> {
  const startTime = Date.now();
  const pollPeriod = 20 * 1000;

  while (true) {
    await page.reload({ waitUntil: ['load', 'domcontentloaded'], timeout: 60 * 1000 });

    if ((await isSecuritySuspended(page)) === suspended) {
      // Success
      break;
    }

    if (Date.now() - startTime > timeOut - pollPeriod) {
      throw new Error('timed out waiting for security suspension status = ' + suspended);
    }
    await page.waitForTimeout(pollPeriod);
  }
}
