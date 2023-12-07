import { RuntimeConfigurationType, RuntimeStatus } from 'generated/fetch';

import { expectButtonElementEnabled } from '../../testing/react-test-helpers';
import { stubDisk } from '../../testing/stubs/disks-api-stub';
import { runtimePresets } from '../utils/runtime-presets';

import {
  defaultGceRuntime,
  defaultGceRuntimeWithPd,
} from 'testing/stubs/runtime-api-stub';

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
    const expected = undefined;

    expect(
      deriveCurrentRuntime({
        crFromCustomRuntimeHook: undefined,
        gcePersistentDisk: undefined,
      })
    ).toEqual(expected);
  });

  it('returns the runtime from the hook if it is not DELETED', () => {
    const runtime = {
      ...defaultGceRuntimeWithPd(),
      status: RuntimeStatus.RUNNING,
    };

    expect(
      deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: undefined,
      })
    ).toEqual(runtime);
  });

  it('returns the runtime from the hook if it is DELETED and config type USER_OVERRIDE', () => {
    const runtime = {
      ...defaultGceRuntime(),
      status: RuntimeStatus.DELETED,
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
    };

    expect(
      deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: undefined,
      })
    ).toEqual(runtime);
  });

  it(
    'converts the GCE runtime from the hook to the GCE-with-PD preset if: ' +
      'it is DELETED and config type GENERAL_ANALYSIS, ' +
      'and there is no PD',
    () => {
      const runtime = {
        ...defaultGceRuntime(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      };

      const expectedGceWithPdConfig = {
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
        persistentDisk: {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: undefined, // cleared by applyPresetOverride()
        },
      };

      const currentRuntime = deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: undefined,
      });

      expect(currentRuntime.gceConfig).toBeFalsy();
      expect(currentRuntime.gceWithPdConfig).toEqual(expectedGceWithPdConfig);
    }
  );

  it(
    'converts the GCE runtime from the hook to the GCE-with-PD preset if: ' +
      'it is DELETED and config type GENERAL_ANALYSIS, ' +
      'and there is a PD',
    () => {
      const runtime = {
        ...defaultGceRuntime(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      };

      const disk = { ...stubDisk(), name: 'whatever' };

      const expectedGceWithPdConfig = {
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
        persistentDisk: {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: disk.name,
        },
      };

      const currentRuntime = deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: disk,
      });

      expect(currentRuntime.gceConfig).toBeFalsy();
      expect(currentRuntime.gceWithPdConfig).toEqual(expectedGceWithPdConfig);
    }
  );
});
//
// describe(deriveExistingAC.name, () => {
//   it('is a placeholder test', () => {
//     const dummy: DeriveExistingACProps = {
//       currentRuntime: undefined,
//       gcePersistentDisk: undefined,
//       pendingRuntime: undefined,
//     };
//     const expected = undefined;
//     expect(deriveExistingAC(dummy)).toEqual(expected);
//   });
// });
//
// describe(derivePanelContent.name, () => {
//   it('is a placeholder test', () => {
//     const dummy: DerivePanelProps = {
//       pendingRuntime: undefined,
//       currentRuntime: undefined,
//       runtimeStatus: undefined,
//     };
//     const expected = undefined;
//     expect(derivePanelContent(dummy)).toEqual(expected);
//   });
// });
//
// describe(deriveErrorsAndWarnings.name, () => {
//   it('is a placeholder test', () => {
//     const dummy: DeriveErrorsWarningsProps = {
//       usingInitialCredits: undefined,
//       analysisConfig: undefined,
//     };
//     const expected: DeriveErrorsWarningsResult = {
//       getErrorMessageContent: undefined,
//       getWarningMessageContent: undefined,
//     };
//     expect(deriveErrorsAndWarnings(dummy)).toEqual(expected);
//   });
// });
