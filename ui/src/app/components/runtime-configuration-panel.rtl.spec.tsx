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

// TODO

describe(deriveCurrentRuntime.name, () => {
  it('is a placeholder test', () => {
    const dummy: DeriveCurrentRuntimeProps = {
      crFromCustomRuntimeHook: undefined,
      gcePersistentDisk: undefined,
    };
    const expected = undefined;
    expect(deriveCurrentRuntime(dummy)).toEqual(expected);
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
