import { browser, by, element } from 'protractor';


/** Auto-generated default testing page object. */
export class TestngPage {
  navigateTo() {
    return browser.get('/');
  }

  getParagraphText() {
    return element(by.css('app-root h1')).getText();
  }
}
