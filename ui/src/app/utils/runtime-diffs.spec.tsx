import { GpuConfig } from 'generated/fetch';

import { AnalysisConfig } from './analysis-config';
import {
  AnalysisDiff,
  AnalysisDiffState,
  applyUpdate,
  compareGpu,
  diffsToUpdateMessaging,
  findMostSevereDiffState,
  rebootUpdate,
  recreateEnvAndPDUpdate,
  recreateEnvUpdate,
} from './runtime-diffs';

// only testing the 'diff' field because the others are straightforward
describe(compareGpu.name, () => {
  const baseAnalysisConfig: AnalysisConfig = {
    computeType: undefined, // none of these are used - only gpuConfig
    machine: undefined,
    diskConfig: undefined,
    detachedDisk: undefined,
    dataprocConfig: undefined,
    autopauseThreshold: undefined,
    gpuConfig: undefined, // will be overridden
  };
  const baseGpuConfig: GpuConfig = {
    gpuType: 'the standard one',
    numOfGpus: 1,
  };

  it('returns a NO_CHANGE diff when neither the old or new config specifies GPUs', () =>
    expect(
      compareGpu(
        { ...baseAnalysisConfig, gpuConfig: undefined },
        { ...baseAnalysisConfig, gpuConfig: undefined }
      )?.diff
    ).toEqual(AnalysisDiffState.NO_CHANGE));

  it('returns a NO_CHANGE diff when configs are identical', () =>
    expect(
      compareGpu(
        { ...baseAnalysisConfig, gpuConfig: baseGpuConfig },
        { ...baseAnalysisConfig, gpuConfig: baseGpuConfig }
      )?.diff
    ).toEqual(AnalysisDiffState.NO_CHANGE));

  it('returns a NEEDS_DELETE diff when adding a gpuConfig', () =>
    expect(
      compareGpu(
        { ...baseAnalysisConfig, gpuConfig: undefined },
        { ...baseAnalysisConfig, gpuConfig: baseGpuConfig }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));

  it('returns a NEEDS_DELETE diff when removing a gpuConfig', () =>
    expect(
      compareGpu(
        { ...baseAnalysisConfig, gpuConfig: baseGpuConfig },
        { ...baseAnalysisConfig, gpuConfig: undefined }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));

  it('returns a NEEDS_DELETE diff when GPU type differs', () =>
    expect(
      compareGpu(
        {
          ...baseAnalysisConfig,
          gpuConfig: { ...baseGpuConfig, gpuType: 'red' },
        },
        {
          ...baseAnalysisConfig,
          gpuConfig: { ...baseGpuConfig, gpuType: 'blue' },
        }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));

  it('returns a NEEDS_DELETE diff when GPU count increases', () =>
    expect(
      compareGpu(
        {
          ...baseAnalysisConfig,
          gpuConfig: baseGpuConfig,
        },
        {
          ...baseAnalysisConfig,
          gpuConfig: {
            ...baseGpuConfig,
            numOfGpus: baseGpuConfig.numOfGpus + 1,
          },
        }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));

  it('returns a NEEDS_DELETE diff when GPU count decreases', () =>
    expect(
      compareGpu(
        {
          ...baseAnalysisConfig,
          gpuConfig: baseGpuConfig,
        },
        {
          ...baseAnalysisConfig,
          gpuConfig: {
            ...baseGpuConfig,
            numOfGpus: baseGpuConfig.numOfGpus - 1,
          },
        }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));
});

describe(findMostSevereDiffState.name, () => {
  test.each([
    [[], undefined],
    [[AnalysisDiffState.NEEDS_DELETE], AnalysisDiffState.NEEDS_DELETE],
    [
      [AnalysisDiffState.NEEDS_DELETE, undefined],
      AnalysisDiffState.NEEDS_DELETE,
    ],
    [
      [
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
        AnalysisDiffState.NEEDS_DELETE,
        AnalysisDiffState.NO_CHANGE,
        AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
        AnalysisDiffState.NO_CHANGE,
      ],
      AnalysisDiffState.NEEDS_DELETE,
    ],
    [
      [
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
        AnalysisDiffState.NO_CHANGE,
        AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
      ],
      AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
    ],
    [
      [
        AnalysisDiffState.NO_CHANGE,
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
        AnalysisDiffState.NO_CHANGE,
      ],
      AnalysisDiffState.CAN_UPDATE_IN_PLACE,
    ],
    [[AnalysisDiffState.NO_CHANGE], AnalysisDiffState.NO_CHANGE],
  ])('findMostSevereDiffState(%s) = %s', (diffStates, want) => {
    expect(findMostSevereDiffState(diffStates)).toEqual(want);
  });
});

// diff state combination logic is covered by the findMostSevereDiffState tests
// so here we can use single-element lists as inputs
describe(diffsToUpdateMessaging.name, () => {
  const baseDiff: AnalysisDiff = {
    desc: undefined, // these are not used by this function
    previous: undefined,
    new: undefined,
    diff: undefined, // tests will override
  };

  // diff = [NO_CHANGE, CAN_UPDATE_IN_PLACE] x diskDiff = [all states]
  test.each([
    [AnalysisDiffState.NO_CHANGE, AnalysisDiffState.NO_CHANGE],
    [AnalysisDiffState.NO_CHANGE, AnalysisDiffState.CAN_UPDATE_IN_PLACE],
    [AnalysisDiffState.NO_CHANGE, AnalysisDiffState.CAN_UPDATE_WITH_REBOOT],
    [AnalysisDiffState.NO_CHANGE, AnalysisDiffState.NEEDS_DELETE],
    [AnalysisDiffState.CAN_UPDATE_IN_PLACE, AnalysisDiffState.NO_CHANGE],
    [
      AnalysisDiffState.CAN_UPDATE_IN_PLACE,
      AnalysisDiffState.CAN_UPDATE_IN_PLACE,
    ],
    [
      AnalysisDiffState.CAN_UPDATE_IN_PLACE,
      AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
    ],
    [AnalysisDiffState.CAN_UPDATE_IN_PLACE, AnalysisDiffState.NEEDS_DELETE],
  ])(
    'it returns an applyUpdate when the environment diff is %s and the diskDiff is %',
    (diff, diskDiff) =>
      expect(
        diffsToUpdateMessaging([
          {
            ...baseDiff,
            diff,
            diskDiff,
          },
        ])
      ).toEqual(applyUpdate)
  );

  test.each([
    AnalysisDiffState.NO_CHANGE,
    AnalysisDiffState.CAN_UPDATE_IN_PLACE,
    AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
    AnalysisDiffState.NEEDS_DELETE,
  ])(
    'it returns a rebootUpdate when the environment diff is CAN_UPDATE_WITH_REBOOT and the diskDiff is %s',
    (diskDiff) =>
      expect(
        diffsToUpdateMessaging([
          {
            ...baseDiff,
            diff: AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
            diskDiff,
          },
        ])
      ).toEqual(rebootUpdate)
  );

  test.each([
    AnalysisDiffState.NO_CHANGE,
    AnalysisDiffState.CAN_UPDATE_IN_PLACE,
    AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
  ])(
    'it returns a recreateEnvUpdate when the environment diff is NEEDS_DELETE and the diskDiff is %s',
    (diskDiff) =>
      expect(
        diffsToUpdateMessaging([
          {
            ...baseDiff,
            diff: AnalysisDiffState.NEEDS_DELETE,
            diskDiff,
          },
        ])
      ).toEqual(recreateEnvUpdate)
  );

  it('returns a recreateEnvAndPDUpdate when both the environment diff and disk diff are NEEDS_DELETE', () =>
    expect(
      diffsToUpdateMessaging([
        {
          ...baseDiff,
          diff: AnalysisDiffState.NEEDS_DELETE,
          diskDiff: AnalysisDiffState.NEEDS_DELETE,
        },
      ])
    ).toEqual(recreateEnvAndPDUpdate));
});
