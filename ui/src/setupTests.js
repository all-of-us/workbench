require('mutationobserver-shim');

/**
 * Defines the React 16 Adapter for Enzyme.
 *
 * @link http://airbnb.io/enzyme/docs/installation/#working-with-react-16
 * @copyright 2017 Airbnb, Inc.
 */
const enzyme = require("enzyme");
const Adapter = require("enzyme-adapter-react-16");

const {setupCustomValidators} = require('app/services/setup');
const {stubPopupDimensions} = require('app/components/popups');

setupCustomValidators();
stubPopupDimensions();
enzyme.configure({ adapter: new Adapter() });

global.beforeEach(() => {
  const appRoot = document.createElement('div');
  appRoot.setAttribute('id', 'root');
  document.body.appendChild(appRoot);

  const popupRoot = document.createElement('div');
  popupRoot.setAttribute('id', 'popup-root');
  document.body.appendChild(popupRoot);
});

global.afterEach(() => {
  document.body.removeChild(document.getElementById('root'));
  document.body.removeChild(document.getElementById('popup-root'));
});

