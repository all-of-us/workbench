import { stubDisk } from 'testing/stubs/disks-api-stub';
import {
  defaultDataProcRuntime,
  defaultGceRuntime,
  defaultGceRuntimeWithPd,
} from 'testing/stubs/runtime-api-stub';

import {
  applyPresetOverride,
  maybeWithPersistentDisk,
} from './runtime-presets';

describe(applyPresetOverride.name, () => {});

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
