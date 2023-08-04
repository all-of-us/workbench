import HomePage from 'app/page/home-page';
import { signInWithAccessToken } from 'utils/test-utils';

test('Smoke test', async () => {
  await signInWithAccessToken(page);
  const homePage = new HomePage(page);
  await homePage.waitForLoad();
  expect(await homePage.isLoaded()).toBe(true);
});
