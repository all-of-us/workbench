import * as fp from 'lodash/fp';
import {Page} from 'puppeteer';

const faker = require('faker/locale/en_US');

export function makeString(charLimit?: number) {
  let loremStr = faker.lorem.paragraphs();
  if (charLimit === undefined) {
    return loremStr;
  }
  if (loremStr.length > charLimit) {
    loremStr = loremStr.slice(0, charLimit);
  }
  return loremStr
}

/**
 * Generate a random filename.
 * @param namePrefix
 */
export function makeDateTimeStr(namePrefix: string) {
  const timestamp = new Date().toISOString();
  return `${namePrefix.replace(/\s/g, '')}_${timestamp}`;
}

export function makeWorkspaceName() {
  return `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
}

export const extractPageName = async (page: Page): Promise<string> => {
  const title = await page.title();
  // extract page name from page title.
  // For example: "View Workspaces | [Test] All of Us Researcher Workbench" becomes "View Workspaces"
  const splitValue = fp.zipObject(['name', 'domain'], title.split(' | '));
  return fp.snakeCase(splitValue.name);
}
