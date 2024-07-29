const rtl = require('@testing-library/react');

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

global.beforeEach(() => {
  const appRoot = document.createElement('div');
  appRoot.setAttribute('id', 'root');
  document.body.appendChild(appRoot);

  const popupRoot = document.createElement('div');
  popupRoot.setAttribute('id', 'popup-root');
  document.body.appendChild(popupRoot);

  window.localStorage.clear();
});

global.afterEach(() => {
  // Unmount react components after each test
  unmountCallbacks.forEach((unmount) => unmount());
  unmountCallbacks.splice();

  // Remove this last, as unmounting may check the popup root.
  document.body.removeChild(document.getElementById('root'));
  document.body.removeChild(document.getElementById('popup-root'));
});

rtl.configure({ testIdAttribute: 'data-test-id' });

module.exports = {
  mockNavigate: mockNavigate,
  mockNavigateByUrl: mockNavigateByUrl,
};
