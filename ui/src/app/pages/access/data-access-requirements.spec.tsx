import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import {
  AccessModule,
  AccessModuleStatus,
  InstitutionApi,
  Profile,
  ProfileApi,
} from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { render, RenderResult, screen } from '@testing-library/react';
import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  DARPageMode,
  DATA_ACCESS_REQUIREMENTS_PATH,
  rtAccessRenewalModules,
} from 'app/utils/access-utils';
import { nowPlusDays } from 'app/utils/dates';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  waitAndExecute,
} from 'testing/react-test-helpers';
import { InstitutionApiStub } from 'testing/stubs/institution-api-stub';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';

import {
  allInitialModules,
  DataAccessRequirements,
  getEligibleModules,
  getFocusedModule,
  initialRequiredModules,
} from './data-access-requirements';

const stubProfile = ProfileStubVariables.PROFILE_STUB as Profile;
const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();

const EXPIRY_DAYS = 365;
const oneYearAgo = () => nowPlusDays(-EXPIRY_DAYS);
const oneYearFromNow = () => nowPlusDays(EXPIRY_DAYS);
const oneHourAgo = () => Date.now() - 1000 * 60 * 60;

describe('DataAccessRequirements', () => {
  const component = (pageMode?: string): RenderResult => {
    const path = pageMode
      ? `${DATA_ACCESS_REQUIREMENTS_PATH}?pageMode=${pageMode}`
      : DATA_ACCESS_REQUIREMENTS_PATH;
    delete window.location;
    // @ts-ignore
    window.location = Object.assign(new URL('https://example.org' + path), {
      ancestorOrigins: '',
      assign: jest.fn(),
      reload: jest.fn(),
      replace: jest.fn(),
    });
    return render(
      <MemoryRouter initialEntries={[path]}>
        <DataAccessRequirements hideSpinner={() => {}} showSpinner={() => {}} />
      </MemoryRouter>
    );
  };

  const findModule = (container, module: AccessModule) =>
    container.querySelectorAll(`[data-test-id="module-${module}"]`)[0];
  const findIneligibleModule = (container, module: AccessModule) =>
    container?.querySelector(`[data-test-id="module-${module}-ineligible"]`);

  const findCompleteModule = (container, module: AccessModule) =>
    container?.querySelector(`[data-test-id="module-${module}-complete"]`);
  const findIncompleteModule = (container, module: AccessModule) =>
    container.querySelector(`[data-test-id="module-${module}-incomplete"]`);

  const findNextCtaForModule = (container, module: AccessModule) =>
    findModule(container, module)?.querySelector(
      '[data-test-id="next-module-cta"]'
    );

  const findCompletionBanner = (container) =>
    container?.querySelector('[data-test-id="dar-completed"]');

  const findControlledSignedStepEligible = (container) =>
    container
      ?.querySelector(
        '[title="Section describing whether an institutional agreement has been signed for controlled tier access"]'
      )
      ?.querySelector('[data-test-id="eligible"]');
  const findControlledSignedStepIneligible = (container) =>
    container
      ?.querySelector(
        '[title="Section describing whether an institutional agreement has been signed for controlled tier access"]'
      )
      ?.querySelector('[data-test-id="ineligible"]');

  const findControlledUserEligible = (container) =>
    container
      ?.querySelector(
        '[title="Section describing whether an institution has granted controlled tier access to the current user"]'
      )
      ?.querySelector('[data-test-id="eligible"]');
  const findControlledUserIneligible = (container) =>
    container
      ?.querySelector(
        '[title="Section describing whether an institution has granted controlled tier access to the current user"]'
      )
      ?.querySelector('[data-test-id="ineligible"]');

  const findControlledTierCard = (container) =>
    container?.querySelector('[data-test-id="controlled-card"]');
  const findEligibleText = (container) =>
    container?.querySelector('[data-test-id="eligible-text"]');
  const findIneligibleText = (container) =>
    container?.querySelector('[data-test-id="ineligible-text"]');
  const findClickableModuleText = (container, module: AccessModule) =>
    container?.querySelector(
      `[data-test-id="module-${module}-clickable-text"]`
    );

  const findContactUs = (container) =>
    container?.querySelector('[data-test-id="contact-us"]');

  const findInitialRegistrationHeader = (container) =>
    container?.querySelector('[data-test-id="initial-registration-header"]');

  const findAnnualRenewalHeader = (container) =>
    container?.querySelector('[data-test-id="annual-renewal-header"]');

  const expectPageMode = (container: HTMLElement, pageMode: DARPageMode) =>
    switchCase(
      pageMode,
      [
        DARPageMode.INITIAL_REGISTRATION,
        () => {
          expect(findInitialRegistrationHeader(container)).toBeTruthy();
          expect(findAnnualRenewalHeader(container)).toBeFalsy();
        },
      ],
      [
        DARPageMode.ANNUAL_RENEWAL,
        () => {
          expect(findInitialRegistrationHeader(container)).toBeFalsy();
          expect(findAnnualRenewalHeader(container)).toBeTruthy();
        },
      ]
    );

  const oneExpiredModule = (moduleName: AccessModule): AccessModuleStatus => {
    const expiredTime = oneHourAgo();
    return {
      moduleName,
      completionEpochMillis: expiredTime - 1,
      expirationEpochMillis: expiredTime,
    };
  };

  const expireAllRTModules = () => {
    const { profile } = profileStore.get();

    const newProfile = fp.set(
      'accessModules',
      {
        modules: rtAccessRenewalModules.map(oneExpiredModule),
      },
      profile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  };

  const addOneModule = (newModuleStatus: AccessModuleStatus) => {
    const { profile } = profileStore.get();

    const newProfile: Profile = {
      ...profile,
      accessModules: {
        modules: [...profile.accessModules.modules, newModuleStatus],
      },
    };

    profileStore.set({ profile: newProfile, load, reload, updateCache });
  };

  function updateOneModuleExpirationTime(
    updateModuleName: AccessModule,
    time: number
  ) {
    const oldProfile = profileStore.get().profile;
    const newModules = [
      ...oldProfile.accessModules.modules.filter(
        (m) => m.moduleName !== updateModuleName
      ),
      {
        ...oldProfile.accessModules.modules.find(
          (m) => m.moduleName === updateModuleName
        ),
        completionEpochMillis: time - 1,
        expirationEpochMillis: time,
      } as AccessModuleStatus,
    ];
    const newProfile = fp.set(
      ['accessModules', 'modules'],
      newModules,
      oldProfile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  }

  const removeOneModule = (toBeRemoved: AccessModule) => {
    const oldProfile = profileStore.get().profile;
    const newModules = oldProfile.accessModules.modules.filter(
      (m) => m.moduleName !== toBeRemoved
    );
    const newProfile = fp.set(
      ['accessModules', 'modules'],
      newModules,
      oldProfile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  };

  const setCompletionTimes = (completionFn) => {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(
      (moduleStatus) => ({
        ...moduleStatus,
        completionEpochMillis: completionFn(),
      }),
      oldProfile.accessModules.modules
    );
    const newProfile = fp.set(
      ['accessModules', 'modules'],
      newModules,
      oldProfile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  };

  const setBypassTimes = (bypassFn) => {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(
      (moduleStatus) => ({
        ...moduleStatus,
        bypassEpochMillis: bypassFn(),
      }),
      oldProfile.accessModules.modules
    );
    const newProfile = fp.set(
      ['accessModules', 'modules'],
      newModules,
      oldProfile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  };

  const expectCompletionBanner = () =>
    expect(
      screen.queryAllByText('Thank you for completing all the necessary steps')
        .length
    ).toBe(1);
  const expectNoCompletionBanner = () =>
    expect(
      screen.queryAllByText('Thank you for completing all the necessary steps')
        .length
    ).toBe(0);

  const expectCtRenewalBanner = () =>
    expect(screen.queryAllByText('Controlled Tier Access Renewal').length).toBe(
      1
    );
  const expectNoCtRenewalBanner = () =>
    expect(screen.queryAllByText('Controlled Tier Access Renewal').length).toBe(
      0
    );

  beforeEach(async () => {
    registerApiClient(InstitutionApi, new InstitutionApiStub());
    registerApiClient(ProfileApi, new ProfileApiStub());

    serverConfigStore.set({
      config: { ...defaultServerConfig, unsafeAllowSelfBypass: true },
    });
    profileStore.set({ profile: stubProfile, load, reload, updateCache });
  });

  afterEach(() => {
    // reset to standard behavior after tests which use fake timers
    jest.useRealTimers();
  });

  it('should return all required modules from getEligibleModules by default (all FFs enabled)', () => {
    const enabledModules = getEligibleModules(allInitialModules, stubProfile);
    initialRequiredModules.forEach((module) =>
      expect(enabledModules.includes(module)).toBeTruthy()
    );
  });

  it('should not return the RAS module from getEligibleModules when its feature flag is disabled', () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: false,
      },
    });
    const enabledModules = getEligibleModules(allInitialModules, stubProfile);
    expect(enabledModules.includes(AccessModule.RASLINKLOGINGOV)).toBeFalsy();
  });

  it('should not return the ERA module from getEligibleModules when its feature flag is disabled', () => {
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableEraCommons: false },
    });
    const enabledModules = getEligibleModules(allInitialModules, stubProfile);
    expect(enabledModules.includes(AccessModule.ERA_COMMONS)).toBeFalsy();
  });

  it('should not return the Compliance module from getEligibleModules when its feature flag is disabled', () => {
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableComplianceTraining: false },
    });
    const enabledModules = getEligibleModules(allInitialModules, stubProfile);
    expect(
      enabledModules.includes(AccessModule.COMPLIANCE_TRAINING)
    ).toBeFalsy();
  });

  it('should return the first module (2FA) from getFocusedModule when no modules have been completed', () => {
    const enabledModules = getEligibleModules(
      initialRequiredModules,
      stubProfile
    );
    const activeModule = getFocusedModule(
      enabledModules,
      stubProfile,
      DARPageMode.INITIAL_REGISTRATION
    );

    expect(activeModule).toEqual(initialRequiredModules[0]);
    expect(activeModule).toEqual(enabledModules[0]);

    // update this if the order changes
    expect(activeModule).toEqual(AccessModule.TWOFACTORAUTH);
  });

  it('should return the second module (RAS) from getFocusedModule when the first module (2FA) has been completed', () => {
    const testProfile = {
      ...stubProfile,
      accessModules: {
        modules: [
          { moduleName: AccessModule.TWOFACTORAUTH, completionEpochMillis: 1 },
        ],
      },
    };

    const enabledModules = getEligibleModules(
      initialRequiredModules,
      stubProfile
    );
    const activeModule = getFocusedModule(
      enabledModules,
      testProfile,
      DARPageMode.INITIAL_REGISTRATION
    );

    expect(activeModule).toEqual(initialRequiredModules[1]);
    expect(activeModule).toEqual(enabledModules[1]);

    // update this if the order changes
    expect(activeModule).toEqual(AccessModule.IDENTITY);
  });

  it('should return the second module (RAS) from getFocusedModule when the first module (2FA) has been bypassed', () => {
    const testProfile = {
      ...stubProfile,
      accessModules: {
        modules: [
          { moduleName: AccessModule.TWOFACTORAUTH, bypassEpochMillis: 1 },
        ],
      },
    };

    const enabledModules = getEligibleModules(
      initialRequiredModules,
      stubProfile
    );
    const activeModule = getFocusedModule(
      enabledModules,
      testProfile,
      DARPageMode.INITIAL_REGISTRATION
    );

    expect(activeModule).toEqual(initialRequiredModules[1]);
    expect(activeModule).toEqual(enabledModules[1]);

    // update this if the order changes
    expect(activeModule).toEqual(AccessModule.IDENTITY);
  });

  it(
    'should return the second enabled module (ERA, not RAS) from getFocusedModule' +
      ' when the first module (2FA) has been completed and RAS is disabled',
    () => {
      serverConfigStore.set({
        config: {
          ...defaultServerConfig,
          enableRasLoginGovLinking: false,
        },
      });

      const testProfile = {
        ...stubProfile,
        accessModules: {
          modules: [
            {
              moduleName: AccessModule.TWOFACTORAUTH,
              completionEpochMillis: 1,
            },
          ],
        },
      };

      const enabledModules = getEligibleModules(
        initialRequiredModules,
        stubProfile
      );
      const activeModule = getFocusedModule(
        enabledModules,
        testProfile,
        DARPageMode.INITIAL_REGISTRATION
      );

      // update this if the order changes
      expect(activeModule).toEqual(AccessModule.ERA_COMMONS);

      // 2FA (module 0) is complete, so enabled #1 is active
      expect(activeModule).toEqual(enabledModules[1]);

      // but we skip requiredModules[1] because it's RAS and is not enabled
      expect(activeModule).toEqual(initialRequiredModules[2]);
    }
  );

  it('should return the fourth module (Compliance) from getFocusedModule when the first 3 modules have been completed', () => {
    const testProfile = {
      ...stubProfile,
      accessModules: {
        modules: [
          { moduleName: AccessModule.TWOFACTORAUTH, completionEpochMillis: 1 },
          { moduleName: AccessModule.ERA_COMMONS, completionEpochMillis: 1 },
          {
            moduleName: AccessModule.IDENTITY,
            completionEpochMillis: 1,
          },
        ],
      },
    };

    const enabledModules = getEligibleModules(
      initialRequiredModules,
      stubProfile
    );
    const activeModule = getFocusedModule(
      enabledModules,
      testProfile,
      DARPageMode.INITIAL_REGISTRATION
    );

    expect(activeModule).toEqual(initialRequiredModules[3]);
    expect(activeModule).toEqual(enabledModules[3]);

    // update this if the order changes
    expect(activeModule).toEqual(AccessModule.COMPLIANCE_TRAINING);
  });

  it('should return undefined from getFocusedModule when all modules have been completed', () => {
    const testProfile = {
      ...stubProfile,
      accessModules: {
        modules: initialRequiredModules.map((module) => ({
          moduleName: module,
          completionEpochMillis: 1,
        })),
      },
    };

    const enabledModules = getEligibleModules(
      initialRequiredModules,
      stubProfile
    );
    const activeModule = getFocusedModule(
      enabledModules,
      testProfile,
      DARPageMode.INITIAL_REGISTRATION
    );

    expect(activeModule).toBeUndefined();
  });

  it('should not indicate the RAS module as active when a user has completed it', () => {
    // initially, the user has completed all required modules except RAS (the standard case at RAS launch time)
    const testProfile = {
      ...stubProfile,
      accessModules: {
        modules: [
          { moduleName: AccessModule.TWOFACTORAUTH, completionEpochMillis: 1 },
          { moduleName: AccessModule.ERA_COMMONS, completionEpochMillis: 1 },
          {
            moduleName: AccessModule.COMPLIANCE_TRAINING,
            completionEpochMillis: 1,
          },
          {
            moduleName: AccessModule.DATA_USER_CODE_OF_CONDUCT,
            completionEpochMillis: 1,
          },
        ],
      },
    };

    const enabledModules = getEligibleModules(
      initialRequiredModules,
      stubProfile
    );

    let activeModule = getFocusedModule(
      enabledModules,
      testProfile,
      DARPageMode.INITIAL_REGISTRATION
    );
    expect(activeModule).toEqual(AccessModule.IDENTITY);

    // simulate handleRasCallback() by updating the profile

    const updatedProfile = {
      ...testProfile,
      accessModules: {
        modules: [
          ...testProfile.accessModules.modules,
          {
            moduleName: AccessModule.IDENTITY,
            completionEpochMillis: 1,
          },
        ],
      },
    };

    activeModule = getFocusedModule(
      enabledModules,
      updatedProfile,
      DARPageMode.INITIAL_REGISTRATION
    );
    expect(activeModule).toBeUndefined();
  });

  it('should render all required modules by default (all FFs enabled)', () => {
    const { container } = component();
    allInitialModules.forEach((module) =>
      expect(findModule(container, module)).toBeTruthy()
    );
  });

  it('should not render the ERA module when its feature flag is disabled', () => {
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableEraCommons: false },
    });
    const { container } = component();
    expect(
      findModule(container, AccessModule.ERA_COMMONS)?.parentElement
    ).toBeFalsy();
  });

  it('should not render the Compliance module when its feature flag is disabled', () => {
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableComplianceTraining: false },
    });
    const { container } = component();
    expect(
      findModule(container, AccessModule.COMPLIANCE_TRAINING)?.parentElement
    ).toBeFalsy();
  });

  // Temporary hack Sep 16: when enableRasLoginGovLinking is false, we DO show the module
  // along with an Ineligible icon and some "technical difficulties" text
  // and it never becomes an activeModule
  it('should render the RAS module as ineligible when its feature flag is disabled', () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: false,
      },
    });
    const { container } = component();
    expect(
      findModule(container, AccessModule.IDENTITY).parentElement
    ).toBeTruthy();
    expect(
      findIneligibleModule(container, AccessModule.IDENTITY).parentElement
    ).toBeTruthy();

    expect(
      findCompleteModule(container, AccessModule.IDENTITY)?.parentElement
    ).toBeFalsy();
    expect(
      findIncompleteModule(container, AccessModule.IDENTITY)?.parentElement
    ).toBeFalsy();
  });

  it('should render all required modules as incomplete when the profile accessModules are empty', () => {
    const { container } = component();
    initialRequiredModules.forEach((module) => {
      expect(findIncompleteModule(container, module)).toBeTruthy();

      expect(findCompleteModule(container, module)).toBeFalsy();
      expect(findIneligibleModule(container, module)).toBeFalsy();
    });
    expect(findCompletionBanner(container)).toBeFalsy();
  });

  it('should render all required modules as complete when the profile accessModules are all complete', () => {
    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: initialRequiredModules.map((module) => ({
            moduleName: module,
            completionEpochMillis: 1,
          })),
        },
      },
      load,
      reload,
      updateCache,
    });

    const { container } = component();
    initialRequiredModules.forEach((module) => {
      expect(findCompleteModule(container, module)).toBeTruthy();

      expect(findIncompleteModule(container, module)).toBeFalsy();
      expect(findIneligibleModule(container, module)).toBeFalsy();
    });
    expect(findCompletionBanner(container)).toBeTruthy();
  });

  // RAS launch bug (no JIRA ticket)
  it('should render all modules as complete by transitioning to all complete', async () => {
    // initially, the user has completed all required modules except Identity (the standard case at Identity launch time)

    const allExceptIdentity = initialRequiredModules.filter(
      (m) => m !== AccessModule.IDENTITY
    );
    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: allExceptIdentity.map((module) => ({
            moduleName: module,
            completionEpochMillis: 1,
          })),
        },
      },
      load,
      reload,
      updateCache,
    });

    const { container } = component();
    allExceptIdentity.forEach((module) => {
      expect(findCompleteModule(container, module)).toBeTruthy();

      expect(findIncompleteModule(container, module)).toBeFalsy();
      expect(findIneligibleModule(container, module)).toBeFalsy();
    });

    // RAS is not complete
    expect(findIncompleteModule(container, AccessModule.IDENTITY)).toBeTruthy();

    expect(findCompleteModule(container, AccessModule.IDENTITY)).toBeFalsy();
    expect(findIneligibleModule(container, AccessModule.IDENTITY)).toBeFalsy();

    expect(findCompletionBanner(container)).toBeFalsy();

    // now all required modules are complete

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: initialRequiredModules.map((module) => ({
            moduleName: module,
            completionEpochMillis: 1,
          })),
        },
      },
      load,
      reload,
      updateCache,
    });

    await waitAndExecute();

    initialRequiredModules.forEach((module) => {
      expect(findCompleteModule(container, module)).toBeTruthy();

      expect(findIncompleteModule(container, module)).toBeFalsy();
      expect(findIneligibleModule(container, module)).toBeFalsy();
    });

    expect(findCompletionBanner(container)).toBeTruthy();
  });

  it('should render all required modules as complete when the profile accessModules are all bypassed', () => {
    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: initialRequiredModules.map((module) => {
            return { moduleName: module, bypassEpochMillis: 1 };
          }),
        },
      },
      load,
      reload,
      updateCache,
    });

    const { container } = component();
    initialRequiredModules.forEach((module) => {
      expect(findCompleteModule(container, module)).toBeTruthy();

      expect(findIncompleteModule(container, module)).toBeFalsy();
      expect(findIneligibleModule(container, module)).toBeFalsy();
    });
    expect(findCompletionBanner(container)).toBeTruthy();
  });

  it('should render a mix of complete and incomplete modules, as appropriate', () => {
    const requiredModulesSize = initialRequiredModules.length;
    const incompleteModules = [AccessModule.IDENTITY];
    const completeModules = initialRequiredModules.filter(
      (module) => module !== AccessModule.IDENTITY
    );
    const completeModulesSize = requiredModulesSize - incompleteModules.length;

    // sanity check
    expect(incompleteModules.length).toEqual(1);
    expect(completeModules.length).toEqual(completeModulesSize);

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: completeModules.map((module) => {
            return { moduleName: module, completionEpochMillis: 1 };
          }),
        },
      },
      load,
      reload,
      updateCache,
    });

    const { container } = component();
    incompleteModules.forEach((module) => {
      expect(findIncompleteModule(container, module)).toBeTruthy();

      expect(findCompleteModule(container, module)).toBeFalsy();
      expect(findIneligibleModule(container, module)).toBeFalsy();
    });
    completeModules.forEach((module) => {
      expect(findCompleteModule(container, module)).toBeTruthy();

      expect(findIncompleteModule(container, module)).toBeFalsy();
      expect(findIneligibleModule(container, module)).toBeFalsy();
    });
    expect(findCompletionBanner(container)).toBeFalsy();
  });

  it('should not show self-bypass UI when unsafeSelfBypass is false', () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        unsafeAllowSelfBypass: false,
      },
    });
    const { container } = component();
    expect(
      container.querySelectorAll('[data-test-id="self-bypass"]').length > 0
    ).toBeFalsy();
  });

  it('should show self-bypass when unsafeSelfBypass is true', () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        unsafeAllowSelfBypass: true,
      },
    });
    const { container } = component();
    expect(
      container.querySelectorAll('[data-test-id="self-bypass"]').length > 0
    ).toBeTruthy();
  });

  it('should not show self-bypass UI when all clickable modules are complete', () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        unsafeAllowSelfBypass: true,
      },
    });
    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: initialRequiredModules.map((module) => {
            return { moduleName: module, completionEpochMillis: 1 };
          }),
        },
      },
      load,
      reload,
      updateCache,
    });

    const { container } = component();
    expect(
      container.querySelectorAll('[data-test-id="self-bypass"]').length > 0
    ).toBeFalsy();
  });

  it('should show self-bypass UI when optional modules are still pending', () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        unsafeAllowSelfBypass: true,
      },
    });
    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: initialRequiredModules.map((module) => {
            return { moduleName: module, completionEpochMillis: 1 };
          }),
        },
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Controlled,
            eraRequired: true,
            eligible: true,
          },
          {
            accessTierShortName: AccessTierShortNames.Registered,
            eraRequired: false,
            eligible: true,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });

    const { container } = component();
    expect(
      container.querySelectorAll('[data-test-id="self-bypass"]').length > 0
    ).toBeTruthy();
  });

  // regression tests for RW-7384: sync external modules to gain access

  it('should sync incomplete external modules', async () => {
    // profile contains no completed modules, so we sync all (2FA, ERA, Compliance)
    const spy2FA = jest.spyOn(profileApi(), 'syncTwoFactorAuthStatus');
    const spyERA = jest.spyOn(profileApi(), 'syncEraCommonsStatus');
    const spyCompliance = jest.spyOn(
      profileApi(),
      'syncComplianceTrainingStatus'
    );

    component();

    expect(spy2FA).toHaveBeenCalledTimes(1);
    expect(spyERA).toHaveBeenCalledTimes(1);
    expect(spyCompliance).toHaveBeenCalledTimes(1);
  });

  it('should not sync complete external modules', async () => {
    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: initialRequiredModules.map((module) => ({
            moduleName: module,
            completionEpochMillis: 1,
          })),
        },
      },
      load,
      reload,
      updateCache,
    });

    const spy2FA = jest.spyOn(profileApi(), 'syncTwoFactorAuthStatus');
    const spyERA = jest.spyOn(profileApi(), 'syncEraCommonsStatus');
    const spyCompliance = jest.spyOn(
      profileApi(),
      'syncComplianceTrainingStatus'
    );

    component();

    expect(spy2FA).toHaveBeenCalledTimes(0);
    expect(spyERA).toHaveBeenCalledTimes(0);
    expect(spyCompliance).toHaveBeenCalledTimes(0);
  });

  it('Should not show Era Commons Module for Registered Tier if the institution does not require eRa', async () => {
    let { container } = component();

    expect(findModule(container, AccessModule.ERA_COMMONS)).toBeTruthy();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Registered,
            eraRequired: false,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(findModule(container, AccessModule.ERA_COMMONS)).toBeFalsy();

    // Ignore eraRequired if the accessTier is Controlled
    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Registered,
            eraRequired: true,
          },
          {
            accessTierShortName: AccessTierShortNames.Controlled,
            eraRequired: false,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(findModule(container, AccessModule.ERA_COMMONS)).toBeTruthy();
  });

  it('Should display Institution has signed agreement when the user has a Tier Eligibility object for CT', async () => {
    let { container } = component();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Controlled,
            eraRequired: true,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(findControlledSignedStepEligible(container)).toBeTruthy();
    expect(findControlledSignedStepIneligible(container)).toBeFalsy();

    // but this is not enough; the user needs to be made eligible by email as well
    expect(findEligibleText(findControlledTierCard(container))).toBeFalsy();
    expect(findIneligibleText(findControlledTierCard(container))).toBeTruthy();
  });

  it("Should not display Institution has signed agreement when the user doesn't have a Tier Eligibility object for CT", async () => {
    let { container } = component();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Registered,
            eraRequired: true,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(findControlledSignedStepEligible(container)).toBeFalsy();
    expect(findControlledSignedStepIneligible(container)).toBeTruthy();

    // but this is not enough; the user needs to be made eligible by email as well
    expect(findEligibleText(findControlledTierCard(container))).toBeFalsy();
    expect(findIneligibleText(findControlledTierCard(container))).toBeTruthy();
  });

  it("Should display Institution allows you to access CT when the user's CT Tier Eligibility object has eligible=true", async () => {
    let { container } = component();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Controlled,
            eraRequired: true,
            eligible: true,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(findControlledUserEligible(container)).toBeTruthy();
    expect(findControlledUserIneligible(container)).toBeFalsy();

    expect(findEligibleText(findControlledTierCard(container))).toBeTruthy();
    expect(findIneligibleText(findControlledTierCard(container))).toBeFalsy();
  });

  it("Should not display Institution allows you to access CT when the user's CT Tier Eligibility object has eligible=false", async () => {
    let { container } = component();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Controlled,
            eraRequired: true,
            eligible: false,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(findControlledUserEligible(container)).toBeFalsy();
    expect(findControlledUserIneligible(container)).toBeTruthy();

    expect(findEligibleText(findControlledTierCard(container))).toBeFalsy();
    expect(findIneligibleText(findControlledTierCard(container))).toBeTruthy();
  });

  it('Should not display Institution allows you to access CT when the user does not have a CT Tier Eligibility object', async () => {
    let { container } = component();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        // no CT eligibility object
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Registered,
            eraRequired: true,
            eligible: false,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(findControlledUserEligible(container)).toBeFalsy();
    expect(findControlledUserIneligible(container)).toBeTruthy();

    expect(findEligibleText(findControlledTierCard(container))).toBeFalsy();
    expect(findIneligibleText(findControlledTierCard(container))).toBeTruthy();
  });

  it('Should display the CT card when the environment has a Controlled Tier', async () => {
    const { container } = component();

    expect(findControlledTierCard(container)).toBeTruthy();
  });

  it(
    'Should display eraCommons module in CT card ' +
      'when the user institution has signed agreement and CT requires eraCommons and RT does not',
    async () => {
      let { container } = component();

      profileStore.set({
        profile: {
          ...ProfileStubVariables.PROFILE_STUB,
          tierEligibilities: [
            {
              accessTierShortName: AccessTierShortNames.Controlled,
              eraRequired: true,
              eligible: true,
            },
            {
              accessTierShortName: AccessTierShortNames.Registered,
              eraRequired: false,
              eligible: false,
            },
          ],
        },
        load,
        reload,
        updateCache,
      });
      ({ container } = component());

      expect(
        findModule(findControlledTierCard(container), AccessModule.ERA_COMMONS)
      ).toBeTruthy();
    }
  );

  it(
    'Should not display eraCommons module in CT card ' +
      'when RT requires eraCommons',
    async () => {
      let { container } = component();

      profileStore.set({
        profile: {
          ...ProfileStubVariables.PROFILE_STUB,
          tierEligibilities: [
            {
              accessTierShortName: AccessTierShortNames.Registered,
              eraRequired: true,
              eligible: false,
            },
            {
              accessTierShortName: AccessTierShortNames.Controlled,
              eraRequired: true,
              eligible: true,
            },
          ],
        },
        load,
        reload,
        updateCache,
      });
      ({ container } = component());

      expect(
        findModule(findControlledTierCard(container), AccessModule.ERA_COMMONS)
      ).toBeFalsy();
    }
  );

  it(
    'Should not display eraCommons module in CT card ' +
      "when user's institution has not signed CT Institution agreement",
    async () => {
      let { container } = component();

      profileStore.set({
        profile: {
          ...ProfileStubVariables.PROFILE_STUB,
          // no CT eligibility object
          tierEligibilities: [
            {
              accessTierShortName: AccessTierShortNames.Registered,
              eraRequired: false,
              eligible: false,
            },
          ],
        },
        load,
        reload,
        updateCache,
      });
      ({ container } = component());

      expect(
        findModule(findControlledTierCard(container), AccessModule.ERA_COMMONS)
      ).toBeFalsy();
    }
  );

  it(
    'Should display ineligible CT Compliance Training module in CT card ' +
      "when user's institution has not signed CT Institution agreement",
    async () => {
      let { container } = component();

      profileStore.set({
        profile: {
          ...ProfileStubVariables.PROFILE_STUB,
          // no CT eligibility object
          tierEligibilities: [
            {
              accessTierShortName: AccessTierShortNames.Registered,
              eraRequired: false,
              eligible: false,
            },
          ],
        },
        load,
        reload,
        updateCache,
      });
      ({ container } = component());

      expect(
        findIneligibleModule(
          findControlledTierCard(container),
          AccessModule.CTCOMPLIANCETRAINING
        )
      ).toBeTruthy();
    }
  );

  it(
    'Should display ineligible CT Compliance Training module in CT card ' +
      'when user is not eligible for CT',
    async () => {
      let { container } = component();

      profileStore.set({
        profile: {
          ...ProfileStubVariables.PROFILE_STUB,
          tierEligibilities: [
            {
              accessTierShortName: AccessTierShortNames.Registered,
              eraRequired: false,
              eligible: true,
            },
            {
              accessTierShortName: AccessTierShortNames.Controlled,
              eraRequired: false,
              // User not eligible for CT i.e user email doesnt match
              // Institution's Controlled Tier email list
              eligible: false,
            },
          ],
        },
        load,
        reload,
        updateCache,
      });
      ({ container } = component());

      expect(
        findIneligibleModule(
          findControlledTierCard(container),
          AccessModule.CTCOMPLIANCETRAINING
        )
      ).toBeTruthy();
    }
  );

  it(
    'Should not display eraCommons module in CT card ' +
      'when eraCommons is disabled via the environment config',
    async () => {
      serverConfigStore.set({
        config: { ...defaultServerConfig, enableEraCommons: false },
      });

      let { container } = component();

      profileStore.set({
        profile: {
          ...ProfileStubVariables.PROFILE_STUB,
          tierEligibilities: [
            {
              accessTierShortName: AccessTierShortNames.Registered,
              eraRequired: false,
              eligible: false,
            },
            {
              accessTierShortName: AccessTierShortNames.Controlled,
              eraRequired: true,
              eligible: true,
            },
          ],
        },
        load,
        reload,
        updateCache,
      });
      ({ container } = component());

      expect(
        findModule(findControlledTierCard(container), AccessModule.ERA_COMMONS)
      ).toBeFalsy();
    }
  );

  it('Should show the IDENTITY help text component when it is incomplete', async () => {
    const { container } = component();

    expect(findCompleteModule(container, AccessModule.IDENTITY)).toBeFalsy();
    expect(findIncompleteModule(container, AccessModule.IDENTITY)).toBeTruthy();

    expect(findContactUs(container)).toBeTruthy();
  });

  it('Should not show the IDENTITY help text component when it is complete', async () => {
    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: [
            {
              moduleName: AccessModule.IDENTITY,
              completionEpochMillis: 1,
            },
          ],
        },
      },
      load,
      reload,
      updateCache,
    });

    const { container } = component();

    expect(findCompleteModule(container, AccessModule.IDENTITY)).toBeTruthy();
    expect(findIncompleteModule(container, AccessModule.IDENTITY)).toBeFalsy();

    expect(findContactUs(container)).toBeFalsy();
  });

  it(
    'Should not display ct Compliance Training module in CT card ' +
      'when enableComplianceTraining is false',
    async () => {
      serverConfigStore.set({
        config: { ...defaultServerConfig, enableComplianceTraining: false },
      });

      let { container } = component();

      profileStore.set({
        profile: {
          ...ProfileStubVariables.PROFILE_STUB,
          tierEligibilities: [
            {
              accessTierShortName: AccessTierShortNames.Registered,
              eraRequired: false,
              eligible: false,
            },
            {
              accessTierShortName: AccessTierShortNames.Controlled,
              eraRequired: true,
              eligible: true,
            },
          ],
        },
        load,
        reload,
        updateCache,
      });
      ({ container } = component());

      expect(
        findModule(
          findControlledTierCard(container),
          AccessModule.CTCOMPLIANCETRAINING
        )
      ).toBeFalsy();
    }
  );

  it('Should display CT training when ineligible', async () => {
    let { container } = component();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Registered,
            eraRequired: false,
            eligible: false,
          },
          {
            accessTierShortName: AccessTierShortNames.Controlled,
            eraRequired: true,
            eligible: false,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(
      findIneligibleModule(container, AccessModule.CTCOMPLIANCETRAINING)
    ).toBeTruthy();
  });

  it('Should display CT training when no institutional DUA', async () => {
    let { container } = component();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Registered,
            eraRequired: false,
            eligible: false,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(
      findIneligibleModule(container, AccessModule.CTCOMPLIANCETRAINING)
    ).toBeTruthy();
  });

  it('Should display CT training when eligible', async () => {
    let { container } = component();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Registered,
            eraRequired: false,
            eligible: false,
          },
          {
            accessTierShortName: AccessTierShortNames.Controlled,
            eraRequired: true,
            eligible: true,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    expect(
      findIncompleteModule(container, AccessModule.CTCOMPLIANCETRAINING)
    ).toBeTruthy();
  });

  it('Should allow CT and DUCC to be simultaneously clickable', async () => {
    let { container } = component();

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: allInitialModules.map((moduleName) => {
            if (
              [
                AccessModule.CTCOMPLIANCETRAINING,
                AccessModule.DATA_USER_CODE_OF_CONDUCT,
              ].includes(moduleName)
            ) {
              return { moduleName };
            }
            return { moduleName, completionEpochMillis: 1 };
          }),
        },
        tierEligibilities: [
          {
            accessTierShortName: AccessTierShortNames.Registered,
            eraRequired: false,
            eligible: false,
          },
          {
            accessTierShortName: AccessTierShortNames.Controlled,
            eraRequired: true,
            eligible: true,
          },
        ],
      },
      load,
      reload,
      updateCache,
    });
    ({ container } = component());

    // Both are clickable.
    expect(
      findClickableModuleText(container, AccessModule.CTCOMPLIANCETRAINING)
    ).toBeTruthy();
    expect(
      findClickableModuleText(container, AccessModule.DATA_USER_CODE_OF_CONDUCT)
    ).toBeTruthy();

    // Only the first module is active.
    console.error(
      "Bob's burgers: ",
      findNextCtaForModule(container, AccessModule.CTCOMPLIANCETRAINING)
    );
    expect(
      findNextCtaForModule(container, AccessModule.CTCOMPLIANCETRAINING)
    ).toBeTruthy();
    expect(
      findNextCtaForModule(container, AccessModule.DATA_USER_CODE_OF_CONDUCT)
    ).toBeFalsy();
  });

  it('Should render in INITIAL_REGISTRATION mode by default', async () => {
    const { container } = component();

    expectPageMode(container, DARPageMode.INITIAL_REGISTRATION);
  });

  it('Should render in ANNUAL_RENEWAL mode when specified by query param', async () => {
    const { container } = component(DARPageMode.ANNUAL_RENEWAL);

    expectPageMode(container, DARPageMode.ANNUAL_RENEWAL);
  });

  it('Should render in INITIAL_REGISTRATION mode if the queryParam is invalid', async () => {
    const { container } = component('some-garbage');

    expectPageMode(container, DARPageMode.INITIAL_REGISTRATION);
  });

  // ACCESS_RENEWAL specific tests

  it('should show the correct state when all RT modules are complete', async () => {
    expireAllRTModules();

    updateOneModuleExpirationTime(
      AccessModule.PROFILE_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATION_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.COMPLIANCE_TRAINING,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.DATA_USER_CODE_OF_CONDUCT,
      oneYearFromNow()
    );

    setCompletionTimes(() => Date.now());

    component(DARPageMode.ANNUAL_RENEWAL);

    expectCompletionBanner();
  });

  it('should show the correct state when all RT modules are expired', async () => {
    setCompletionTimes(oneYearAgo);
    expireAllRTModules();
    component(DARPageMode.ANNUAL_RENEWAL);

    expect(screen.queryAllByText('Review').length).toBe(1);
    expect(screen.queryAllByText('Confirm').length).toBe(1);
    expect(screen.queryAllByText('View & Sign').length).toBe(1);
    expect(screen.queryAllByText('Complete Training').length).toBe(1);

    expectNoCompletionBanner();
  });

  it('should show the correct state when all RT modules are incomplete', async () => {
    component(DARPageMode.ANNUAL_RENEWAL);

    expect(screen.queryAllByText('Review').length).toBe(1);
    expect(screen.queryAllByText('Confirm').length).toBe(1);
    expect(screen.queryAllByText('View & Sign').length).toBe(1);
    expect(screen.queryAllByText('Complete Training').length).toBe(1);

    expectNoCompletionBanner();
  });

  it('should show the correct state when RT and CT modules are complete', async () => {
    expireAllRTModules();
    addOneModule(oneExpiredModule(AccessModule.CTCOMPLIANCETRAINING));

    updateOneModuleExpirationTime(
      AccessModule.PROFILE_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATION_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.COMPLIANCE_TRAINING,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.DATA_USER_CODE_OF_CONDUCT,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.CTCOMPLIANCETRAINING,
      oneYearFromNow()
    );

    setCompletionTimes(() => Date.now());

    component(DARPageMode.ANNUAL_RENEWAL);

    expectCompletionBanner();
    expectNoCtRenewalBanner();
  });

  it('should show the correct state when RT=complete and CT=expired', async () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        unsafeAllowSelfBypass: true,
      },
    });

    expireAllRTModules();
    addOneModule(oneExpiredModule(AccessModule.CTCOMPLIANCETRAINING));

    updateOneModuleExpirationTime(
      AccessModule.PROFILE_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATION_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.COMPLIANCE_TRAINING,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.DATA_USER_CODE_OF_CONDUCT,
      oneYearFromNow()
    );

    setCompletionTimes(() => Date.now());

    component(DARPageMode.ANNUAL_RENEWAL);

    expectNoCompletionBanner();
    expectCtRenewalBanner();
  });

  it('should show the correct state when profile confirmation is complete', async () => {
    expireAllRTModules();

    updateOneModuleExpirationTime(
      AccessModule.PROFILE_CONFIRMATION,
      oneYearFromNow()
    );
    component(DARPageMode.ANNUAL_RENEWAL);

    // Complete
    expect(screen.queryAllByText('Confirmed').length).toBe(1);

    // Incomplete
    expect(screen.queryAllByText('Confirm').length).toBe(1);
    expect(screen.queryAllByText('View & Sign').length).toBe(1);
    expect(screen.queryAllByText('Complete Training').length).toBe(1);

    expectNoCompletionBanner();
  });

  it('should show the correct state when profile and publication confirmations are complete', async () => {
    expireAllRTModules();

    updateOneModuleExpirationTime(
      AccessModule.PROFILE_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATION_CONFIRMATION,
      oneYearFromNow()
    );

    component(DARPageMode.ANNUAL_RENEWAL);

    // Complete
    expect(screen.queryAllByText('Confirmed').length).toBe(2);

    // Incomplete
    expect(screen.queryAllByText('View & Sign').length).toBe(1);
    expect(screen.queryAllByText('Complete Training').length).toBe(1);

    expectNoCompletionBanner();
  });

  it('should show the correct state when all RT modules except DUCC are complete', async () => {
    expireAllRTModules();

    updateOneModuleExpirationTime(
      AccessModule.PROFILE_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATION_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.COMPLIANCE_TRAINING,
      oneYearFromNow()
    );

    component(DARPageMode.ANNUAL_RENEWAL);

    // Complete
    expect(screen.queryAllByText('Confirmed').length).toBe(2);
    expect(screen.queryAllByText('Completed').length).toBe(1);
    // Incomplete
    expect(screen.queryAllByText('View & Sign').length).toBe(1);

    expectNoCompletionBanner();
  });

  it('should ignore modules which are not expirable', async () => {
    expireAllRTModules();

    const newModules = [
      ...profileStore.get().profile.accessModules.modules,
      {
        moduleName: AccessModule.TWOFACTORAUTH, // not expirable
        completionEpochMillis: null,
        bypassEpochMillis: null,
        expirationEpochMillis: oneYearAgo(),
      },
    ];

    const newProfile: Profile = {
      ...profileStore.get().profile,
      accessModules: { modules: newModules },
    };

    profileStore.set({ profile: newProfile, load, reload, updateCache });

    updateOneModuleExpirationTime(
      AccessModule.PROFILE_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATION_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.COMPLIANCE_TRAINING,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.DATA_USER_CODE_OF_CONDUCT,
      oneYearFromNow()
    );

    setCompletionTimes(() => Date.now());

    component(DARPageMode.ANNUAL_RENEWAL);

    // All Complete
    expect(screen.queryAllByText('Confirmed').length).toBe(2);
    expect(screen.queryAllByText('Completed').length).toBe(2);

    expectCompletionBanner();
  });

  it('should show the correct state when modules are bypassed', async () => {
    expireAllRTModules();

    setBypassTimes(() => Date.now());

    component(DARPageMode.ANNUAL_RENEWAL);

    expect(screen.queryAllByText('Bypassed').length).toBe(4);
    expect(screen.queryAllByText('(bypassed)', { exact: false }).length).toBe(
      4
    );

    expectCompletionBanner();
  });

  it('should show the correct state when modules are disabled', async () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableComplianceTraining: false,
      },
    });

    setCompletionTimes(() => Date.now());

    updateOneModuleExpirationTime(
      AccessModule.PROFILE_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATION_CONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.DATA_USER_CODE_OF_CONDUCT,
      oneYearFromNow()
    );

    // this module will not be returned in AccessModules because it is disabled
    removeOneModule(AccessModule.COMPLIANCE_TRAINING);

    component(DARPageMode.ANNUAL_RENEWAL);

    // profileConfirmation, publicationConfirmation, and DUCC are complete
    expect(screen.queryAllByText('Confirmed').length).toBe(2);
    expect(screen.queryAllByText('Completed').length).toBe(1);
    expect(
      screen.queryAllByText(`${EXPIRY_DAYS - 1} days`, { exact: false }).length
    ).toBe(3);

    // complianceTraining is not shown because it is disabled
    expect(screen.queryAllByText('Complete Training').length).toBe(0);

    // all of the necessary steps = 3 rather than the usual 4
    expectCompletionBanner();
  });

  // RW-7473: sync expiring/expired Training module to gain access
  test.each([
    ['should', 'expired', 1, oneHourAgo()],
    ['should', 'expiring', 1, nowPlusDays(10)],
    ['should not', 'complete', 0, oneYearFromNow()],
    ['should not', 'incomplete', 0, null],
  ])(
    '%s externally sync %s Compliance Training module status',
    async (desc1, desc2, expected, expirationTime) => {
      const spy = jest.spyOn(profileApi(), 'syncComplianceTrainingStatus');

      updateOneModuleExpirationTime(
        AccessModule.COMPLIANCE_TRAINING,
        expirationTime
      );

      component(DARPageMode.ANNUAL_RENEWAL);

      expect(spy).toHaveBeenCalledTimes(expected);
    }
  );

  it('should allow completion of profile and publication confirmations when incomplete', async () => {
    removeOneModule(AccessModule.PROFILE_CONFIRMATION);
    removeOneModule(AccessModule.PUBLICATION_CONFIRMATION);

    const { container } = component(DARPageMode.ANNUAL_RENEWAL);

    // all are Incomplete
    expect(screen.queryAllByText('Review').length).toBe(1);
    expect(screen.queryAllByText('Confirm').length).toBe(1);
    expect(screen.queryAllByText('View & Sign').length).toBe(1);
    expect(screen.queryAllByText('Complete Training').length).toBe(1);

    expectNoCompletionBanner();
    expectButtonElementEnabled(screen.queryByText('Review'));

    // not yet - need to click a radio button
    expectButtonElementDisabled(screen.queryByText('Confirm'));

    (
      container.querySelectorAll(
        '[data-test-id="report-submitted"]'
      )[0] as HTMLElement
    ).click();

    expectButtonElementEnabled(screen.queryByText('Confirm'));
  });
});
