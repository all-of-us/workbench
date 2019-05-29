import {BreadcrumbType} from 'app/utils/navigation';

import {getTrail} from './breadcrumb';

describe('getTrail', () => {
  it('works', () => {
    const trail = getTrail(BreadcrumbType.Participant, {
      workspace: {name: 'TestW'},
      cohort: {name: 'TestC'},
      urlParams: {ns: 'testns', wsid: 'testwsid', cid: 88, pid: 77}
    });
    expect(trail.map(item => item.label))
      .toEqual(['Workspaces', 'TestW', 'Cohorts', 'TestC', 'Participant 77']);
    expect(trail[4].url)
      .toEqual('/workspaces/testns/testwsid/cohorts/88/review/participants/77')
  });
});
