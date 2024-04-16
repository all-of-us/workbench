import '@testing-library/jest-dom';

import * as React from 'react';

import { Domain } from 'generated/fetch';

import { render, screen } from '@testing-library/react';

import { SelectionList } from './selection-list';

describe('SelectionList', () => {
  it('should create', () => {
    render(
      <SelectionList
        back={() => {}}
        close={() => {}}
        disableFinish={false}
        domain={Domain.CONDITION}
        finish={() => {}}
        removeSelection={() => {}}
        selections={[]}
        setView={() => {}}
        view={''}
      />
    );
    screen.getByRole('img', {
      name: /close/i,
    });
  });
});
