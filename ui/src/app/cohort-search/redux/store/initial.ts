import {fromJS} from 'immutable';

export const SR_ID = 'searchReqest0';

/**
 * InitialState
 */
export const initialState = fromJS({
  entities: {
    searchRequests: {
      [SR_ID]: {
        includes: ['include0'],
        excludes: ['exclude0'],
        count: 0,
        isRequesting: false
      }
    },
    groups: {
      include0: {
        id: 'include0',
        items: [],
        count: null,
        isRequesting: false,
      },
      exclude0: {
        id: 'exclude0',
        items: [],
        count: null,
        isRequesting: false,
      },
    },
    items: {},
    criteria: {},
  },

  wizard: {
    open: false,
    item: {},
    selections: {},
  },

  criteria: {
    tree: {},
    requests: {},
  },
});
