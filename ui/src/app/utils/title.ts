import {environment} from 'environments/environment';

import {BASE_TITLE} from 'app/utils/strings';

export function buildPageTitleForEnvironment(pageTitle?: string) {
  let title = BASE_TITLE;
  if (environment.shouldShowDisplayTag) {
    title = `[${environment.displayTag}] ${title}`;
    if (pageTitle) {
      title = `${pageTitle} | ${title}`;
    }
  }
  return title;
}
