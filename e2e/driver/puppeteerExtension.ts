const Page = require('puppeteer/lib/Page');
const ElementHandle = require( 'puppeteer/lib/JSHandle' ).ElementHandle;
import AouElement from './AouElement';


declare module 'puppeteer' {
  interface ElementHandle {
    asAouElement: () => AouElement;
    getAttribute: (attr: string) => Promise<unknown>;
    clicking: () => Promise<unknown>;
  }
}


ElementHandle.prototype.asAouElement = function (): AouElement {
  return new AouElement(this.asElement());
};

/**
 * Get element attribute
 * @param {string} attr
 * @returns {Promise<String>}
 */
ElementHandle.prototype.getAttribute = async function ( attributeName ): Promise<unknown> {
  const handle = await this._page.evaluateHandle( ( el, attr ) => el.getAttribute(attr), this, attributeName );
  return await handle.jsonValue();
};


ElementHandle.prototype.clicking = async function (): Promise<unknown> {
  return await this._page.evaluate( el => el.click(), this );
};

