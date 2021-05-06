import * as fp from 'lodash/fp';
import { Page } from 'puppeteer';

const faker = require('faker/locale/en_US');

export function makeString(charLimit?: number): string {
  let loremStr: string = faker.lorem.paragraphs();
  if (charLimit === undefined) {
    return loremStr;
  }
  if (loremStr.length > charLimit) {
    loremStr = loremStr.slice(0, charLimit);
  }
  return loremStr;
}

export function makeUrl(charLimit?: number): string {
  const randomPage = makeString(charLimit).replace(/\s/g, '');
  return `http://example.com/${randomPage}.html`;
}

/**
 * Generate a random filename.
 * @param namePrefix
 */
export function makeDateTimeStr(namePrefix: string): string {
  const timestamp = new Date().toISOString();
  return `${namePrefix.replace(/\s/g, '')}_${timestamp}`;
}

export function makeWorkspaceName(): string {
  return `aoutest-${Math.floor(Math.random() * 100000)}${Math.floor(Date.now() / 1000)}`;
}

export function makeRandomName(prefix?: string): string {
  prefix = prefix || 'aoutest';
  return `${prefix}-${Math.floor(Math.random() * 10000000000)}`;
}

export const extractPageName = async (page: Page): Promise<string> => {
  const title = await page.title();
  // extract page name from page title.
  // For example: "View Workspaces | [Test] All of Us Researcher Workbench" becomes "View Workspaces"
  const splitValue = fp.zipObject(['name', 'domain'], title.split(' | '));
  return splitValue.name.replace(/\s/g, '');
};

/**
 * Get the Workspace namespace from page URL.
 * @param url
 */
export function extractNamespace(url: URL): string {
  const urlPath = url.pathname;
  return urlPath.split('/')[2];
}
