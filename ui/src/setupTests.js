import { configure } from '@testing-library/react';
import { stubPopupDimensions } from 'app/components/popups';
import { setupCustomValidators } from 'app/services/setup';

setupCustomValidators();
stubPopupDimensions();

export const mockNavigate = jest.fn();
export const mockNavigateByUrl = jest.fn();

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
  // Remove this last, as unmounting may check the popup root.
  document.body.removeChild(document.getElementById('root'));
  document.body.removeChild(document.getElementById('popup-root'));
});

configure({ testIdAttribute: 'data-test-id' });
