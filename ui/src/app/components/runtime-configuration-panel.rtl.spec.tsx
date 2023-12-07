import {
  DeriveConfigProps,
  deriveConfiguration,
  DerivedConfigResult,
  deriveErrorsAndWarnings,
  DeriveErrorsWarningsProps,
  DeriveErrorsWarningsResult,
  derivePanelContent,
  DerivePanelProps,
} from './runtime-configuration-panel';

// TODO

describe(deriveConfiguration.name, () => {
  it('is a placeholder test', () => {
    const dummy: DeriveConfigProps = {
      crFromCustomRuntimeHook: undefined,
      gcePersistentDisk: undefined,
      pendingRuntime: undefined,
    };
    const expected: DerivedConfigResult = {
      currentRuntime: undefined,
      existingAnalysisConfig: undefined,
    };
    expect(deriveConfiguration(dummy)).toEqual(expected);
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
