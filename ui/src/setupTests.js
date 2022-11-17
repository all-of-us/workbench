/**
 * Defines the React 16 Adapter for Enzyme.
 *
 * @link http://airbnb.io/enzyme/docs/installation/#working-with-react-16
 * @copyright 2017 Airbnb, Inc.
 */
const enzyme = require('enzyme');
const Adapter = require('@wojtekmaj/enzyme-adapter-react-17');

const { setupCustomValidators } = require('app/services/setup');
const { stubPopupDimensions } = require('app/components/popups');

setupCustomValidators();
stubPopupDimensions();

const mockNavigate = jest.fn();
const mockNavigateByUrl = jest.fn();

jest.mock('app/utils/navigation', () => ({
  ...jest.requireActual('app/utils/navigation'),
  __esModule: true,
  useNavigation: () => [mockNavigate, mockNavigateByUrl],
}));

// Track all enzyme renderers for teardown.
// See https://github.com/enzymejs/enzyme/issues/911
const unmountCallbacks = [];

class ReactAdapterWithMountTracking extends Adapter {
  constructor(...args) {
    super(...args);
  }

  createRenderer(...args) {
    const renderer = Adapter.prototype.createRenderer.call(this, ...args);
    unmountCallbacks.push(() => renderer.unmount());
    return renderer;
  }
}

global.beforeEach(() => {
  const appRoot = document.createElement('div');
  appRoot.setAttribute('id', 'root');
  document.body.appendChild(appRoot);

  const popupRoot = document.createElement('div');
  popupRoot.setAttribute('id', 'popup-root');
  document.body.appendChild(popupRoot);
});

global.afterEach(() => {
  // Unmount react components after each test
  unmountCallbacks.forEach((unmount) => unmount());
  unmountCallbacks.splice();

  // Remove this last, as unmounting may check the popup root.
  document.body.removeChild(document.getElementById('root'));
  document.body.removeChild(document.getElementById('popup-root'));
});

enzyme.configure({ adapter: new ReactAdapterWithMountTracking() });

module.exports = {
  mockNavigate: mockNavigate,
  mockNavigateByUrl: mockNavigateByUrl,
};
