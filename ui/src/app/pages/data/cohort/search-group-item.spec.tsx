import '@testing-library/jest-dom';

import * as React from 'react';

import { Domain } from 'generated/fetch';

import { render, screen } from '@testing-library/react';

import { SearchGroupItem } from './search-group-item';

const itemStub = {
  id: 'item_id',
  searchParameters: [],
  status: 'active',
  type: Domain.CONDITION,
};

const workspaceStub = {
  id: 'workspace_id',
  namespace: 'namespace',
};

describe('SearchGroupItem', () => {
  it('should render', () => {
    render(
      <SearchGroupItem
        role='includes'
        groupId='group_id'
        item={itemStub}
        workspace={workspaceStub}
        index={0}
        updateGroup={() => {}}
      />
    );
    expect(screen.getByText('Contains Conditions Code')).toBeInTheDocument();
  });
});
