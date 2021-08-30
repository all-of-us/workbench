export const mockNavigate = jest.fn();
export const mockNavigateByUrl = jest.fn();

jest.mock('app/utils/navigation', () => ({
  ...jest.requireActual('app/utils/navigation') as any,
  __esModule: true,
  useNavigation: () => [mockNavigate, mockNavigateByUrl],
}));
