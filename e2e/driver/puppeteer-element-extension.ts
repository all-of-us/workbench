const ElementHandle = require( 'puppeteer/lib/JSHandle' ).ElementHandle;
import AouElement from './AouElement';


declare module 'puppeteer' {

  interface ElementHandle {
    asAouElement: () => AouElement;
  }
}


ElementHandle.prototype.asAouElement = function() {
  return new AouElement((this.asElement()));
};
