import {
  AnalysisDiff,
  AnalysisDiffState,
  applyUpdate,
  diffsToUpdateMessaging,
  findMostSevereDiffState,
  rebootUpdate,
  recreateEnvAndPDUpdate,
  recreateEnvUpdate,
} from './runtime-diff-utils';

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
