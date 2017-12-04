import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {fromJS, List} from 'immutable';

/* tslint:disable-next-line:no-unused-variable */
import {CohortSearchState, initialState, SR_ID} from '../store';
import {CohortSearchActions} from './service';

/**
 * Dummy objects / mock state peices
 */
const dummyItem = fromJS({
  id: 'item001',
  type: 'icd9',
  searchParameters: ['param0', 'param1'],
  modifiers: [],
  count: null,
  isRequesting: false,
});

const zeroCrit = fromJS({
  id: 0,
  parameterId: 'param0',
  name: 'CodeA',
  code: 'CodeA',
  type: 'icd9',
  group: false,
  domainId: null,
});

const oneCrit = fromJS({
  id: 1,
  parameterId: 'param1',
  name: 'CodeB',
  code: 'CodeB',
  type: 'icd9',
  group: true,
  domainId: null,
});

const DEMO_crit = fromJS({
  id: 3,
  parameterId: 'paramF',
  name: 'Female',
  type: 'DEMO',
  subtype: 'GEN',
  code: 'F',
  group: false,
  conceptId: 12345,
  domainId: null,
  attribute: null,
});

const dummyState = initialState
  .setIn(['entities', 'groups', 'include0', 'items'], List([dummyItem.get('id')]))
  .setIn(['entities', 'parameters', 'param0'], zeroCrit)
  .setIn(['entities', 'parameters', 'param1'], oneCrit)
  .setIn(['entities', 'items', dummyItem.get('id')], dummyItem);

const expectedSR = {
  includes: [{
    id: 'include0',
    items: [{
      id: 'item001',
      type: 'ICD9',
      searchParameters: [{
          parameterId: 'param0',
          name: 'CodeA',
          value: 'CodeA',
          type: 'icd9',
          subtype: '',
          group: false,
          domain: null,
        }, {
          parameterId: 'param1',
          name: 'CodeB',
          value: 'CodeB',
          type: 'icd9',
          subtype: '',
          group: true,
          domain: null,
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
    expect(requestSpy).toHaveBeenCalledWith('searchRequests', SR_ID, expectedSR);
    expect(setDataSpy).not.toHaveBeenCalled();
  });

  /* If the group being updated is the only group, then whether it has a
    * count or not, we should request totals from the API
    */
  const _requestTotalCountWithOneGroup = (mockStore) => {
    const requestSpy = spyOn(actions, 'requestCharts');
    const setDataSpy = spyOn(actions, 'setChartData');
    mockReduxInst.getState = () => mockStore;
    actions.requestTotalCount('include0');
    expect(requestSpy).toHaveBeenCalledWith('searchRequests', SR_ID, expectedSR);
    expect(setDataSpy).not.toHaveBeenCalled();
  };

  it('requestTotalCount(id): ignore group is only group, count is null', () => {
    // dummyState already has null for group count and just a single group
    const mockStore = dummyState;
    _requestTotalCountWithOneGroup(mockStore);
  });

  it('requestTotalCount(id): ignore group is only group, count is zero', () => {
    const mockStore = dummyState.setIn(['entities', 'groups', 'include0', 'count'], 0);
    _requestTotalCountWithOneGroup(mockStore);
  });

  it('requestTotalCount(id): ignore group is only group, count is real', () => {
    const mockStore = dummyState.setIn(['entities', 'groups', 'include0', 'count'], 123);
    _requestTotalCountWithOneGroup(mockStore);
  });

  it('requestTotalCount(id): group given is not only group', () => {
    const secondItem = dummyItem.set('id', 'item002');
    const secondGroup = fromJS({
      id: 'include1',
      items: ['item002'],
      count: null,
      isRequesting: false,
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
        items: [{
          id: 'item001',
          type: 'ICD9',
          searchParameters: [{
              parameterId: 'param0',
              name: 'CodeA',
              value: 'CodeA',
              type: 'icd9',
              subtype: '',
              group: false,
              domain: null,
            }, {
              parameterId: 'param1',
              name: 'CodeB',
              value: 'CodeB',
              type: 'icd9',
              subtype: '',
              group: true,
              domain: null,
          }],
          modifiers: [],
        }]
      }, {
        id: 'include1',
        items: [{
          id: 'item002',
          type: 'ICD9',
          searchParameters: [{
              parameterId: 'param0',
              name: 'CodeA',
              value: 'CodeA',
              type: 'icd9',
              subtype: '',
              group: false,
              domain: null,
            }, {
              parameterId: 'param1',
              name: 'CodeB',
              value: 'CodeB',
              type: 'icd9',
              subtype: '',
              group: true,
              domain: null,
          }],
          modifiers: [],
        }]
      }],
      excludes: [],
    };

    // Othe group has null count
    mockReduxInst.getState = () => twoGroupState;
    actions.requestTotalCount('include0');
    expect(requestSpy).toHaveBeenCalledWith('searchRequests', SR_ID, newExpectedSR);
    expect(setDataSpy).not.toHaveBeenCalled();

    requestSpy.calls.reset();
    setDataSpy.calls.reset();

    const groupCountPath = ['entities', 'groups', 'include1', 'count'];

    // Other group has nonzero real count
    mockReduxInst.getState = () => twoGroupState.setIn(groupCountPath, 123);
    actions.requestTotalCount('include0');
    expect(requestSpy).toHaveBeenCalledWith('searchRequests', SR_ID, newExpectedSR);
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
      type: 'icd9',
      subtype: '',
      group: false,
      domain: null,
    });
    // Demographics
    const demoParam = actions.mapParameter(DEMO_crit);
    expect(demoParam).toEqual({
      parameterId: 'paramF',
      name: 'Female',
      value: 'F',
      type: 'DEMO',
      subtype: 'GEN',
      group: false,
      conceptId: 12345,
      attribute: null,
    });
  });
});
