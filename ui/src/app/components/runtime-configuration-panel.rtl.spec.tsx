import { RuntimeConfigurationType, RuntimeStatus } from 'generated/fetch';

import { expectButtonElementEnabled } from '../../testing/react-test-helpers';
import { stubDisk } from '../../testing/stubs/disks-api-stub';
import { runtimePresets } from '../utils/runtime-presets';

import {
  defaultDataProcRuntime,
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

  describe.each([
    ['GCE', defaultGceRuntime()],
    ['GCE with PD', defaultGceRuntimeWithPd()],
    ['DataProc', defaultDataProcRuntime()],
  ])('%s', (desc, runtime) => {
    it(`returns a ${desc} runtime from the hook if it is not DELETED`, () => {
      // sanity check
      expect(runtime.status).not.toEqual(RuntimeStatus.DELETED);
      expect(
        deriveCurrentRuntime({
          crFromCustomRuntimeHook: runtime,
          gcePersistentDisk: undefined,
        })
      ).toEqual(runtime);
    });

    it(`returns a ${desc} runtime from the hook if it is DELETED and config type USER_OVERRIDE`, () => {
      const testRuntime = {
        ...runtime,
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.USER_OVERRIDE,
      };

      expect(
        deriveCurrentRuntime({
          crFromCustomRuntimeHook: testRuntime,
          gcePersistentDisk: undefined,
        })
      ).toEqual(testRuntime);
    });
  });

  it(
    'converts a GCE runtime from the hook to the GCE-with-PD preset if: ' +
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
      expect(currentRuntime.dataprocConfig).toBeFalsy();
    }
  );

  it(
    'converts a GCE runtime from the hook to the GCE-with-PD preset if: ' +
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
      expect(currentRuntime.dataprocConfig).toBeFalsy();
    }
  );

  it(
    'converts the GCE-with-PD runtime from the hook to the preset if: ' +
      'it is DELETED and config type GENERAL_ANALYSIS, ' +
      'and there is a PD',
    () => {
      const runtimeDiskName = 'something';
      const pdName = 'something else';

      const runtime = {
        ...defaultGceRuntimeWithPd(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
        gceWithPdConfig: {
          ...defaultGceRuntimeWithPd().gceWithPdConfig,
          persistentDisk: {
            ...defaultGceRuntimeWithPd().gceWithPdConfig.persistentDisk,
            name: runtimeDiskName,
          },
        },
      };

      const disk = { ...stubDisk(), name: pdName };

      const expectedGceWithPdConfig = {
        ...runtime.gceWithPdConfig,
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
        persistentDisk: {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: runtimeDiskName, // keeps original disk, does NOT attach a different one
        },
      };

      const currentRuntime = deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: disk,
      });

      expect(currentRuntime.gceConfig).toBeFalsy();
      expect(currentRuntime.gceWithPdConfig).toEqual(expectedGceWithPdConfig);
      expect(currentRuntime.dataprocConfig).toBeFalsy();
    }
  );

  it(
    'converts the GCE-with-PD runtime from the hook to the preset if: ' +
      'it is DELETED and config type GENERAL_ANALYSIS, ' +
      'and there is no PD',
    () => {
      const runtime = {
        ...defaultGceRuntimeWithPd(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      };

      const expectedGceWithPdConfig = {
        ...runtime.gceWithPdConfig,
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
        persistentDisk: {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: runtime.gceWithPdConfig.persistentDisk.name,
        },
      };

      const currentRuntime = deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: undefined,
      });

      expect(currentRuntime.gceConfig).toBeFalsy();
      expect(currentRuntime.gceWithPdConfig).toEqual(expectedGceWithPdConfig);
      expect(currentRuntime.dataprocConfig).toBeFalsy();
    }
  );

  it('converts the Dataproc runtime from the hook to the preset if it is DELETED and config type HAIL_GENOMIC_ANALYSIS', () => {
    const runtime = {
      ...defaultDataProcRuntime(),
      status: RuntimeStatus.DELETED,
      configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
    };

    const currentRuntime = deriveCurrentRuntime({
      crFromCustomRuntimeHook: runtime,
      gcePersistentDisk: undefined,
    });

    expect(currentRuntime.gceConfig).toBeFalsy();
    expect(currentRuntime.gceWithPdConfig).toBeFalsy();
    expect(currentRuntime.dataprocConfig).toEqual(
      runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
    );
  });
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
