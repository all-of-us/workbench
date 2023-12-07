import { RuntimeConfigurationType, RuntimeStatus } from 'generated/fetch';

import { runtimePresets } from 'app/utils/runtime-presets';

import { stubDisk } from 'testing/stubs/disks-api-stub';
import {
  defaultDataProcRuntime,
  defaultGceRuntime,
  defaultGceRuntimeWithPd,
  defaultRuntime,
} from 'testing/stubs/runtime-api-stub';

import {
  createOrCustomize,
  deriveCurrentRuntime,
  deriveErrorsAndWarnings,
} from './runtime-configuration-panel';
import { PanelContent } from './runtime-configuration-panel/utils';

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

describe(createOrCustomize.name, () => {
  it('returns Customize when a pendingRuntime exists', () => {
    expect(
      createOrCustomize({
        pendingRuntime: defaultRuntime(),
        currentRuntime: undefined,
        runtimeStatus: undefined,
      })
    ).toEqual(PanelContent.Customize);
  });

  test.each([
    ['null', null, undefined],
    ['undefined', undefined, undefined],
    ['UNKNOWN', defaultRuntime(), RuntimeStatus.UNKNOWN],
    [
      'DELETED GCE GENERAL_ANALYSIS', // not GCE with PD
      {
        ...defaultGceRuntime(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      },
      RuntimeStatus.DELETED, // not used here, but let's be consistent
    ],
    [
      'DELETED HAIL_GENOMIC_ANALYSIS',
      {
        ...defaultDataProcRuntime(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
      },
      RuntimeStatus.DELETED, // not used here, but let's be consistent
    ],
  ])(
    'it returns Create for a %s runtime',
    (desc, currentRuntime, runtimeStatus) => {
      expect(
        createOrCustomize({
          pendingRuntime: undefined,
          currentRuntime,
          runtimeStatus,
        })
      ).toEqual(PanelContent.Create);
    }
  );
});

describe(deriveErrorsAndWarnings.name, () => {
  it('is a placeholder test', () => {
    const { getErrorMessageContent, getWarningMessageContent } =
      deriveErrorsAndWarnings({
        usingInitialCredits: undefined,
        analysisConfig: undefined,
      });

    expect(getErrorMessageContent()).toBeFalsy();
    expect(getWarningMessageContent()).toBeFalsy();
  });
});
