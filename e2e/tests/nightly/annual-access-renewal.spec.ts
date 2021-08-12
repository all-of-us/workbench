import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import AccessRenewalPage from 'app/page/access-renewal-page';
import ProfilePage from 'app/page/profile-page';
import ProfileConfirmationModal from 'app/modal/profile-confirmation-modal';
import Navigation, { NavLink } from 'app/component/navigation';
import HomePage from 'app/page/home-page';

// Important: the access test user must be in a state where they are currently failing access renewal
// due to an expired "profile last confirmed" date.  This is accomplished by the CircleCI nightly test runner
// executing the API project.rb CLI tool SetAccessModuleTimestamps

describe('Annual Access Renewal', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page, config.ACCESS_TEST_ACCESS_TOKEN_FILE);
  });

  test('Expired User is redirected to Annual Access Renewal (AAR) on login', async () => {
    const aarPage = await new AccessRenewalPage(page).waitForLoad();
    expect(aarPage).toBeTruthy();
    expect(await aarPage.hasExpired()).toBeTruthy();
  });

  // note that this test is "destructive" in that it brings the user to a state
  // where they cannot complete this test again, because they have completed
  // AAR and are no longer forced into renewal

  test('Expired User can complete Annual Access Renewal (AAR)', async () => {
    const aarPage = await new AccessRenewalPage(page).waitForLoad();
    expect(aarPage).toBeTruthy();
    expect(await aarPage.hasExpired()).toBeTruthy();

    // the profile confirmation is expired, so the Review action is active for the profile

    const reviewButton = await aarPage.getReviewProfileButton();
    expect(reviewButton).toBeTruthy();

    // clicking Review redirects the user to the Profile page

    await reviewButton.click();
    const profilePage = await new ProfilePage(page).waitForLoad();

    // the Profile page indicates that confirmation is needed, and provides a link to confirm

    expect(await profilePage.needsConfirmation()).toBeTruthy();
    const looksGoodLink = await profilePage.getLooksGoodLink();

    // clicking Looks Good shows the Profile Confirmation modal

    await looksGoodLink.click();
    const modal = new ProfileConfirmationModal(page);
    expect(await modal.waitForLoad()).toBeTruthy();
    const modalOK = modal.getOKButton();

    // clicking OK returns us to AAR

    await modalOK.click();
    await aarPage.waitForLoad();
    expect(await aarPage.hasExpired()).toBeFalsy();

    // and we can now access the home page

    await Navigation.navMenu(page, NavLink.HOME);
    const homePage = await new HomePage(page).waitForLoad();
    expect(homePage).toBeTruthy();
  });
});
