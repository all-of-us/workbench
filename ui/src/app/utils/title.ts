import {environment} from 'environments/environment';

import {BASE_TITLE} from 'app/utils/strings';

export function buildPageTitleForEnvironment(pageTitle?: string) {
  let title = BASE_TITLE;
  if (pageTitle) {
    title = `${pageTitle} | ${title}`;
  }
  if (environment.shouldShowDisplayTag) {
    title = `[${environment.displayTag}] ${title}`;
  }
  return title;
}
