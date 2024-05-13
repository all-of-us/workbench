import * as React from 'react';

import { CohortBuilderApi, Domain } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { SearchGroup } from './search-group';

const itemsStub = [
  {
    id: 'itemA',
    type: Domain.MEASUREMENT,
    searchParameters: [],
    modifiers: [],
    count: 1,
    temporalGroup: 0,
    isRequesting: false,
    status: 'active',
  },
  {
    id: 'itemB',
    type: Domain.MEASUREMENT,
    searchParameters: [],
    modifiers: [],
    count: 2,
    temporalGroup: 0,
    isRequesting: false,
    status: 'active',
  },
  {
    id: 'itemC',
    type: Domain.MEASUREMENT,
    searchParameters: [],
    modifiers: [],
    count: 3,
    temporalGroup: 1,
    isRequesting: false,
    status: 'active',
  },
];
const groupStub = {
  id: 'group_id',
  items: itemsStub,
  status: 'active',
  type: Domain.CONDITION,
};
describe('SearchGroup', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
    serverConfigStore.set({ config: defaultServerConfig });
  });

  it('should render', async () => {
    render(
      <SearchGroup
        role='includes'
        group={groupStub}
        index={0}
        updateRequest={() => {}}
      />
    );
    await screen.findByText(/group count:/i);
  });

  it('Should render each item in items prop', async () => {
    const { rerender } = render(
      <SearchGroup
        role='includes'
        group={groupStub}
        index={0}
        updateRequest={() => {}}
      />
    );
    expect((await screen.findAllByTestId('item-list')).length).toBe(
      itemsStub.length
    );
    rerender(
      <SearchGroup
        role='includes'
        group={{ ...groupStub, temporal: true }}
        index={0}
        updateRequest={() => {}}
      />
    );
    // Should split the items by temporalGroup when temporal is true
    expect((await screen.findAllByTestId('item-list')).length).toBe(
      itemsStub.filter((it) => it.temporalGroup === 0).length
    );
    expect((await screen.findAllByTestId('temporal-item-list')).length).toBe(
      itemsStub.filter((it) => it.temporalGroup === 1).length
    );
  });
});
