import {fromJS} from 'immutable';

export const SR_ID = 'searchRequest0';

/**
 * InitialState
 */
export const initialState = fromJS({
  entities: {
    searchRequests: {
      [SR_ID]: {
        includes: [],
        excludes: [],
        count: 0,
        isRequesting: false
      }
    },
    groups: {},
    items: {},
    parameters: {},
  },

  wizard: {
    open: false,
    item: {},
    selections: {},
    focused: {},
  },

  criteria: {
    tree: {},
    requests: {},
    errors: {},
  },

  chartData: [],

  initShowChart: false,

});
