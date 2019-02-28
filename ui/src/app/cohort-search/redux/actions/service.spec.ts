import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {fromJS, List} from 'immutable';

import {CohortSearchState, initialState, SR_ID} from 'app/cohort-search/redux/store';
import {TreeType} from 'generated';
import {CohortSearchActions} from './service';

/**
 * Dummy objects / mock state peices
 */
const CDR_VERSION_ID = 1;

const dummyItem = fromJS({
  id: 'item001',
  type: TreeType[TreeType.ICD9],
  searchParameters: ['param0', 'param1'],
  modifiers: [],
  count: null,
  isRequesting: false,
  status: 'active',
});

const zeroCrit = fromJS({
  id: 0,
  parameterId: 'param0',
  name: 'CodeA',
  code: 'CodeA',
  type: TreeType[TreeType.ICD9],
  group: false,
  conceptId: 12345
}).set('attributes', []);

const oneCrit = fromJS({
  id: 1,
  parameterId: 'param1',
  name: 'CodeB',
  code: 'CodeB',
  type: TreeType[TreeType.ICD9],
  group: true,
  conceptId: 12345
}).set('attributes', []);

const DEMO_crit = fromJS({
  id: 3,
  parameterId: 'paramF',
  name: 'Female',
  type: TreeType[TreeType.DEMO],
  subtype: 'GEN',
  code: 'F',
  group: false,
  conceptId: 12345,
  domainId: null,
}).set('attributes', []);

const groups = fromJS({
  include0: {
    id: 'include0',
    temporal: false,
    items: [],
    count: null,
    isRequesting: false,
    status: 'active',
  },
  exclude0: {
    id: 'exclude0',
    temporal: false,
    items: [],
    count: null,
    isRequesting: false,
  },
});

const dummyState = initialState
  .setIn(['entities', 'groups'], groups)
  .setIn(['entities', 'searchRequests', SR_ID, 'includes'], List(['include0']))
  .setIn(['entities', 'searchRequests', SR_ID, 'excludes'], List(['exclude0']))
  .setIn(['entities', 'groups', 'include0', 'items'], List([dummyItem.get('id')]))
  .setIn(['entities', 'parameters', 'param0'], zeroCrit)
  .setIn(['entities', 'parameters', 'param1'], oneCrit)
  .setIn(['entities', 'items', dummyItem.get('id')], dummyItem);

const expectedSR = {
  includes: [{
    id: 'include0',
    temporal: false,
    items: [{
      id: 'item001',
      type: TreeType[TreeType.ICD9],
      searchParameters: [{
        parameterId: 'param0',
        name: 'CodeA',
        value: 'CodeA',
        type: TreeType[TreeType.ICD9],
        subtype: '',
        group: false,
        attributes: [],
        conceptId: 12345
      }, {
        parameterId: 'param1',
        name: 'CodeB',
        value: 'CodeB',
        type: TreeType[TreeType.ICD9],
        subtype: '',
        group: true,
        attributes: [],
        conceptId: 12345
      }],
      modifiers: [],
    }]
  }],
  excludes: [],
};


/**
 *  Test Battery
 */
describe('CohortSearchActions', () => {
  let actions: CohortSearchActions;
  let mockReduxInst: MockNgRedux;

  beforeEach(() => {
    MockNgRedux.reset();
    mockReduxInst = MockNgRedux.getInstance();
    mockReduxInst.getState = () => dummyState;
    actions = new CohortSearchActions(
      mockReduxInst as NgRedux<CohortSearchState>,
    );
    actions.cdrVersionId = CDR_VERSION_ID;
  });

  it('Should correctly be monkey-patched', () => {
    // Verifies that monkey-patching works the way we're doing it
    // Monkey Patch the state
    mockReduxInst.getState = () => dummyState;
    // Check that the monkey patch worked correctly
    expect(actions.state).toEqual(dummyState);
  });

  it('cancelIfRequesting');
  it('removeGroup');
  it('removeGroupItem');
  it('fetchCriteria');
  it('requestItemCount');
  it('requestGroupCount');

  it('requestTotalCount(): no ignore group', () => {
    const requestSpy = spyOn(actions, 'requestCharts');
    const setDataSpy = spyOn(actions, 'setChartData');
    actions.requestTotalCount();
    expect(requestSpy).toHaveBeenCalledWith(CDR_VERSION_ID, 'searchRequests', SR_ID, expectedSR);
    expect(setDataSpy).not.toHaveBeenCalled();
  });

  /* If the group being updated is the only group, then whether it has a
    * count or not, we should request totals from the API
    */
  const requestTotalCountWithOneGroup = (mockStore) => {
    const requestSpy = spyOn(actions, 'requestCharts');
    const setDataSpy = spyOn(actions, 'setChartData');
    mockReduxInst.getState = () => mockStore;
    actions.requestTotalCount('include0');
    expect(requestSpy).toHaveBeenCalledWith(CDR_VERSION_ID, 'searchRequests', SR_ID, expectedSR);
    expect(setDataSpy).not.toHaveBeenCalled();
  };

  it('requestTotalCount(id): ignore group is only group, count is null', () => {
    // dummyState already has null for group count and just a single group
    const mockStore = dummyState;
    requestTotalCountWithOneGroup(mockStore);
  });

  it('requestTotalCount(id): ignore group is only group, count is zero', () => {
    const mockStore = dummyState.setIn(['entities', 'groups', 'include0', 'count'], 0);
    requestTotalCountWithOneGroup(mockStore);
  });

  it('requestTotalCount(id): ignore group is only group, count is real', () => {
    const mockStore = dummyState.setIn(['entities', 'groups', 'include0', 'count'], 123);
    requestTotalCountWithOneGroup(mockStore);
  });

  it('requestTotalCount(id): group given is not only group', () => {
    const secondItem = dummyItem.set('id', 'item002');
    const secondGroup = fromJS({
      id: 'include1',
      temporal: false,
      items: ['item002'],
      count: null,
      isRequesting: false,
      status: 'active',
    });
    const twoGroupState = dummyState
      .updateIn(
        ['entities', 'searchRequests', SR_ID, 'includes'],
        incList => incList.push('include1')
      )
      .setIn(['entities', 'groups', 'include1'], secondGroup)
      .setIn(['entities', 'items', 'item002'], secondItem);

    const requestSpy = spyOn(actions, 'requestCharts');
    const setDataSpy = spyOn(actions, 'setChartData');
    const newExpectedSR = {
      includes: [{
        id: 'include0',
        temporal: false,
        items: [{
          id: 'item001',
          type: TreeType[TreeType.ICD9],
          searchParameters: [{
            parameterId: 'param0',
            name: 'CodeA',
            value: 'CodeA',
            type: TreeType[TreeType.ICD9],
            subtype: '',
            group: false,
            conceptId: 12345,
            attributes: []
          }, {
            parameterId: 'param1',
            name: 'CodeB',
            value: 'CodeB',
            type: TreeType[TreeType.ICD9],
            subtype: '',
            group: true,
            conceptId: 12345,
            attributes: []
          }],
          modifiers: [],
        }]
      }, {
        id: 'include1',
        temporal: false,
        items: [{
          id: 'item002',
          type: TreeType[TreeType.ICD9],
          searchParameters: [{
            parameterId: 'param0',
            name: 'CodeA',
            value: 'CodeA',
            type: TreeType[TreeType.ICD9],
            subtype: '',
            group: false,
            conceptId: 12345,
            attributes: []
          }, {
            parameterId: 'param1',
            name: 'CodeB',
            value: 'CodeB',
            type: TreeType[TreeType.ICD9],
            subtype: '',
            group: true,
            conceptId: 12345,
            attributes: []
          }],
          modifiers: [],
        }]
      }],
      excludes: [],
    };

    // Othe group has null count
    mockReduxInst.getState = () => twoGroupState;
    actions.requestTotalCount('include0');
    expect(requestSpy).toHaveBeenCalledWith(CDR_VERSION_ID, 'searchRequests', SR_ID, newExpectedSR);
    expect(setDataSpy).not.toHaveBeenCalled();

    requestSpy.calls.reset();
    setDataSpy.calls.reset();

    const groupCountPath = ['entities', 'groups', 'include1', 'count'];

    // Other group has nonzero real count
    mockReduxInst.getState = () => twoGroupState.setIn(groupCountPath, 123);
    actions.requestTotalCount('include0');
    expect(requestSpy).toHaveBeenCalledWith(CDR_VERSION_ID, 'searchRequests', SR_ID, newExpectedSR);
    expect(setDataSpy).not.toHaveBeenCalled();

    requestSpy.calls.reset();
    setDataSpy.calls.reset();

    // Other group has zero count
    mockReduxInst.getState = () => twoGroupState.setIn(groupCountPath, 0);
    actions.requestTotalCount('include0');
    expect(requestSpy).not.toHaveBeenCalled();
    expect(setDataSpy).toHaveBeenCalledWith('searchRequests', SR_ID, []);
  });

  it('mapAll', () => {
    expect(actions.mapAll()).toEqual(expectedSR);
  });

  it('mapGroup', () => {
    expect(actions.mapGroup('include0')).toEqual(expectedSR.includes[0]);
  });

  it('mapGroupItem', () => {
    expect(actions.mapGroupItem('item001')).toEqual(expectedSR.includes[0].items[0]);
  });

  it('mapParameter', () => {
    // ICD9, ICD10, CPT
    const zeroParam = actions.mapParameter(zeroCrit);
    expect(zeroParam).toEqual({
      parameterId: 'param0',
      name: 'CodeA',
      value: 'CodeA',
      type: TreeType[TreeType.ICD9],
      subtype: '',
      group: false,
      conceptId: 12345,
      attributes: []
    });
    // Demographics
    const demoParam = actions.mapParameter(DEMO_crit);
    expect(demoParam).toEqual({
      parameterId: 'paramF',
      name: 'Female',
      value: 'F',
      type: TreeType[TreeType.DEMO],
      subtype: 'GEN',
      group: false,
      conceptId: 12345,
      attributes: DEMO_crit.get('attributes'),
    });
  });
});
