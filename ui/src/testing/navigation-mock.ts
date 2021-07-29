export const navigateSpy = jest.fn();
export const navigateByUrlSpy = jest.fn();

jest.mock('app/utils/navigation', () => ({
  ...jest.requireActual('app/utils/navigation'),
  useNavigation: () => [navigateSpy, navigateByUrlSpy],
}));

console.log("this ran");
