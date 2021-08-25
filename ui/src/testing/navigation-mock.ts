import * as ReactRouterDom from 'react-router-dom';

export const navigateSpy = jest.fn();
export const navigateByUrlSpy = jest.fn();

jest.mock('app/utils/navigation', () => ({
  ...jest.requireActual('app/utils/navigation') as typeof ReactRouterDom,
  useNavigation: () => [navigateSpy, navigateByUrlSpy],
}));
