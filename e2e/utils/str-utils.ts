import * as fp from 'lodash/fp';
import { Page } from 'puppeteer';

import faker from 'faker';

// Warning: this function produces values in a fixed range. This has a ~.11%
// chance of creating two identical values; tests should not depend on this
// being random.
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

// Makes a different string of the same length.
export function makeDifferentStringSameLength(original: string): string {
  if (!original) {
    throw Error('cannot make a different empty string');
  }

  let out = '';
  do {
    out = makeString(original.length);
  } while (original === out);
  return out;
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

export function makeWorkspaceName(opts: { includeHyphen?: boolean } = {}): string {
  const { includeHyphen = true } = opts;
  return `aoutest${includeHyphen ? '-' : ''}${Math.floor(Math.random() * 100000)}${Math.floor(Date.now() / 1000)}`;
}

export function makeRandomName(prefix?: string, opts: { includeHyphen?: boolean } = {}): string {
  const { includeHyphen = true } = opts;
  prefix = prefix || 'aoutest';
  return `${prefix}${includeHyphen ? '-' : ''}${Math.floor(Math.random() * 10000000000)}`;
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

export function numericalStringToNumber(value: string): number {
  return Number(value.replace(/,/g, ''));
}

export function isBlank(toTest: string): boolean {
  if (toTest === null || toTest === undefined) {
    return true;
  } else {
    toTest = toTest.trim();
    return toTest === '';
  }
}
