import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {List, fromJS} from 'immutable';

import {initialState, CohortSearchState, SR_ID} from '../store';
import {CohortSearchActions} from './service';

import {CohortBuilderService} from 'generated';

/**
 * Dummy objects / mock state peices
 */
const dummyItem = fromJS({
  id: 'item001',
  type: 'icd9',
  searchParameters: [0, 1],
  modifiers: [],
  count: null,
  isRequesting: false,
});

const zeroCrit = fromJS({
  id: 0,
  type: 'icd9',
  code: 'CodeA',
  domainId: null,
});

const oneCrit = fromJS({
  id: 1,
  type: 'icd9',
  code: 'CodeB',
  domainId: null,
});

const DEMO_crit = fromJS({
  type: 'DEMO',
  subtype: 'GEN',
  code: 'F',
  name: 'Female',
  conceptId: 12345,
  domainId: null,
});

const dummyState = initialState
  .setIn(['entities', 'groups', 'include0', 'items'], List([dummyItem.get('id')]))
  .setIn(['entities', 'criteria', 0], zeroCrit)
  .setIn(['entities', 'criteria', 1], oneCrit)
  .setIn(['entities', 'items', dummyItem.get('id')], dummyItem);

const expectedSR = {
  includes: [{
    items: [{
      type: 'ICD9',
      searchParameters: [{
          value: 'CodeA',
          domain: null,
        }, {
          value: 'CodeB',
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
      {} as CohortBuilderService,
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
    const requestCountSpy = spyOn(actions, 'requestCounts');
    const setCountSpy = spyOn(actions, 'setCount');
    actions.requestTotalCount();
    expect(requestCountSpy).toHaveBeenCalledWith('searchRequests', SR_ID, expectedSR);
    expect(setCountSpy).not.toHaveBeenCalled();
  });

  /* If the group being updated is the only group, then whether it has a
    * count or not, we should request totals from the API
    */
  const _requestTotalCountWithOneGroup = (mockStore) => {
    const requestCountSpy = spyOn(actions, 'requestCounts');
    const setCountSpy = spyOn(actions, 'setCount');
    mockReduxInst.getState = () => mockStore;
    actions.requestTotalCount('include0');
    expect(requestCountSpy).toHaveBeenCalledWith('searchRequests', SR_ID, expectedSR);
    expect(setCountSpy).not.toHaveBeenCalled();
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

    const requestCountSpy = spyOn(actions, 'requestCounts');
    const setCountSpy = spyOn(actions, 'setCount');
    const newExpectedSR = {
      includes: [{
        items: [{
          type: 'ICD9',
          searchParameters: [{
              value: 'CodeA',
              domain: null,
            }, {
              value: 'CodeB',
              domain: null,
            }],
          modifiers: [],
        }]
      }, {
        items: [{
          type: 'ICD9',
          searchParameters: [{
              value: 'CodeA',
              domain: null,
            }, {
              value: 'CodeB',
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
    expect(requestCountSpy).toHaveBeenCalledWith('searchRequests', SR_ID, newExpectedSR);
    expect(setCountSpy).not.toHaveBeenCalled();

    requestCountSpy.calls.reset();
    setCountSpy.calls.reset();

    const groupCountPath = ['entities', 'groups', 'include1', 'count'];

    // Other group has nonzero real count
    mockReduxInst.getState = () => twoGroupState.setIn(groupCountPath, 123);
    actions.requestTotalCount('include0');
    expect(requestCountSpy).toHaveBeenCalledWith('searchRequests', SR_ID, newExpectedSR);
    expect(setCountSpy).not.toHaveBeenCalled();

    requestCountSpy.calls.reset();
    setCountSpy.calls.reset();

    // Other group has zero count
    mockReduxInst.getState = () => twoGroupState.setIn(groupCountPath, 0);
    actions.requestTotalCount('include0');
    expect(requestCountSpy).not.toHaveBeenCalled();
    expect(setCountSpy).toHaveBeenCalledWith('searchRequests', SR_ID, 0);
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
      value: 'CodeA',
      domain: null,
    });
    // Demographics
    const demoParam = actions.mapParameter(DEMO_crit);
    expect(demoParam).toEqual({
      value: 'F',
      subtype: 'GEN',
      conceptId: 12345,
    });
  });
});
