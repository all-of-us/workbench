import { RuntimeConfigurationType, RuntimeStatus } from 'generated/fetch';

import { defaultRuntime } from 'testing/stubs/runtime-api-stub';

import {
  deriveCurrentRuntime,
  DeriveCurrentRuntimeProps,
  deriveErrorsAndWarnings,
  DeriveErrorsWarningsProps,
  DeriveErrorsWarningsResult,
  deriveExistingAC,
  DeriveExistingACProps,
  derivePanelContent,
  DerivePanelProps,
} from './runtime-configuration-panel';

describe(deriveCurrentRuntime.name, () => {
  it('returns an undefined runtime if the inputs are undefined', () => {
    const dummy: DeriveCurrentRuntimeProps = {
      crFromCustomRuntimeHook: undefined,
      gcePersistentDisk: undefined,
    };
    const expected = undefined;
    expect(deriveCurrentRuntime(dummy)).toEqual(expected);
  });

  it('returns the runtime from the hook if it is not DELETED', () => {
    const runtime = {
      ...defaultRuntime(),
      runtimeStatus: RuntimeStatus.RUNNING,
    };
    const dummy: DeriveCurrentRuntimeProps = {
      crFromCustomRuntimeHook: runtime,
      gcePersistentDisk: undefined,
    };
    expect(deriveCurrentRuntime(dummy)).toEqual(runtime);
  });

  it('returns the runtime from the hook if it is DELETED and config type USER_OVERRIDE', () => {
    const runtime = {
      ...defaultRuntime(),
      runtimeStatus: RuntimeStatus.DELETED,
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
    };
    const dummy: DeriveCurrentRuntimeProps = {
      crFromCustomRuntimeHook: runtime,
      gcePersistentDisk: undefined,
    };
    expect(deriveCurrentRuntime(dummy)).toEqual(runtime);
  });
});

describe(deriveExistingAC.name, () => {
  it('is a placeholder test', () => {
    const dummy: DeriveExistingACProps = {
      currentRuntime: undefined,
      gcePersistentDisk: undefined,
      pendingRuntime: undefined,
    };
    const expected = undefined;
    expect(deriveExistingAC(dummy)).toEqual(expected);
  });
});

describe(derivePanelContent.name, () => {
  it('is a placeholder test', () => {
    const dummy: DerivePanelProps = {
      pendingRuntime: undefined,
      currentRuntime: undefined,
      runtimeStatus: undefined,
    };
    const expected = undefined;
    expect(derivePanelContent(dummy)).toEqual(expected);
  });
});

describe(deriveErrorsAndWarnings.name, () => {
  it('is a placeholder test', () => {
    const dummy: DeriveErrorsWarningsProps = {
      usingInitialCredits: undefined,
      analysisConfig: undefined,
    };
    const expected: DeriveErrorsWarningsResult = {
      getErrorMessageContent: undefined,
      getWarningMessageContent: undefined,
    };
    expect(deriveErrorsAndWarnings(dummy)).toEqual(expected);
  });
});
