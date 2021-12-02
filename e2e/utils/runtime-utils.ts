import { Page } from 'puppeteer';
import Button from 'app/element/button';

const Selector = {
  runtimeInitializerCreateButton: '//*[@role="dialog"]//*[text()="Create Environment" and @role="button"]'
};

export async function initializeRuntimeIfModalPresented(page: Page): Promise<void> {
  const initializeButton = new Button(page, Selector.runtimeInitializerCreateButton);
  const existsButton = await initializeButton.exists();
  if (existsButton) {
    await initializeButton.clickAndWait();
  }
}
