import { RuntimeConfigurationType } from 'generated/fetch';

import { stubDisk } from 'testing/stubs/disks-api-stub';
import {
  defaultDataProcRuntime,
  defaultGceRuntime,
  defaultGceRuntimeWithPd,
} from 'testing/stubs/runtime-api-stub';

import {
  applyPresetOverride,
  maybeWithPersistentDisk,
  runtimePresets,
} from './runtime-presets';

describe(applyPresetOverride.name, () => {
  it('removes the gceConfig from a runtime because there is no valid preset for that type', () => {
    const runtime = defaultGceRuntime();
    expect(runtime.gceConfig).toBeTruthy();
    expect(applyPresetOverride(runtime).gceConfig).toBeFalsy();
  });

  it('applies the General Analysis preset override to a GCE with PD runtime when configurationType is GENERAL_ANALYSIS', () => {
    const runtime = {
      ...defaultGceRuntimeWithPd(),
      configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
    };

    // test validity check: the original config differs from the preset, and specifically the PD name
    expect(runtime.gceWithPdConfig).not.toEqual(
      runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
    );
    expect(runtime.gceWithPdConfig.persistentDisk.name).toBeTruthy();
    expect(
      runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
        .persistentDisk.name
    ).toBeFalsy();

    const expectedConfig = {
      ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
      persistentDisk: {
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
          .persistentDisk,
        name: runtime.gceWithPdConfig.persistentDisk.name,
      },
    };

    expect(applyPresetOverride(runtime).gceWithPdConfig).toEqual(
      expectedConfig
    );
  });

  it('does not apply the General Analysis preset override to a GCE with PD runtime when configurationType is USER_OVERRIDE', () => {
    const runtime = {
      ...defaultGceRuntimeWithPd(),
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
    };

    // test validity check: the original config differs from the preset
    expect(runtime.gceWithPdConfig).not.toEqual(
      runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
    );

    expect(applyPresetOverride(runtime).gceWithPdConfig).toEqual(
      runtime.gceWithPdConfig
    );
  });

  it('applies the Hail Genomics Analysis preset override to a Dataproc runtime when configurationType is HAIL_GENOMIC_ANALYSIS', () => {
    const runtime = {
      ...defaultDataProcRuntime(),
      configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
    };

    // test validity check: the original config differs from the preset
    expect(runtime.dataprocConfig).not.toEqual(
      runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
    );

    expect(applyPresetOverride(runtime).dataprocConfig).toEqual(
      runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
    );
  });

  it('does not apply the Hail Genomics Analysis preset override to a Dataproc runtime when configurationType is USER_OVERRIDE', () => {
    const runtime = {
      ...defaultDataProcRuntime(),
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
    };

    // test validity check: the original config differs from the preset
    expect(runtime.dataprocConfig).not.toEqual(
      runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
    );

    expect(applyPresetOverride(runtime).dataprocConfig).toEqual(
      runtime.dataprocConfig
    );
  });
});

describe(maybeWithPersistentDisk.name, () => {
  it('returns the existing runtime when dataproc', () => {
    const runtime = defaultDataProcRuntime();
    const disk = {
      ...stubDisk(),
      name: 'a non default value',
    };
    const newRuntime = maybeWithPersistentDisk(runtime, disk);
    expect(newRuntime).toEqual(runtime);
  });

  it('returns the existing runtime when a PD is already attached', () => {
    const runtime = defaultGceRuntimeWithPd();
    const disk = {
      ...stubDisk(),
      name: 'a non default value',
    };
    const newRuntime = maybeWithPersistentDisk(runtime, disk);
    expect(newRuntime).toEqual(runtime);
  });

  it('returns the existing GCE (no PD) runtime when a the disk is null', () => {
    const runtime = defaultGceRuntime();
    const disk = null;
    const newRuntime = maybeWithPersistentDisk(runtime, disk);
    expect(newRuntime).toEqual(runtime);
  });

  it('returns the existing GCE (no PD) runtime when a the disk is undefined', () => {
    const runtime = defaultGceRuntime();
    const disk = undefined;
    const newRuntime = maybeWithPersistentDisk(runtime, disk);
    expect(newRuntime).toEqual(runtime);
  });

  it('adds a persistent disk to a GCE (no PD) runtime', () => {
    const runtime = defaultGceRuntime();
    const disk = stubDisk();

    const newRuntime = maybeWithPersistentDisk(runtime, disk);

    expect(newRuntime.gceWithPdConfig).toBeTruthy();
    expect(newRuntime.gceConfig).toBeFalsy();
    expect(newRuntime.dataprocConfig).toBeFalsy();

    // fields copied from the original runtime
    expect(newRuntime.runtimeName).toEqual(runtime.runtimeName);
    expect(newRuntime.googleProject).toEqual(runtime.googleProject);
    expect(newRuntime.status).toEqual(runtime.status);
    expect(newRuntime.createdDate).toEqual(runtime.createdDate);
    expect(newRuntime.toolDockerImage).toEqual(runtime.toolDockerImage);
    expect(newRuntime.configurationType).toEqual(runtime.configurationType);

    // fields copied from the disk
    expect(newRuntime.gceWithPdConfig.persistentDisk.size).toEqual(disk.size);
    expect(newRuntime.gceWithPdConfig.persistentDisk.name).toEqual(disk.name);
    expect(newRuntime.gceWithPdConfig.persistentDisk.diskType).toEqual(
      disk.diskType
    );
  });
});
