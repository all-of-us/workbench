import * as React from 'react';

import { ResourceType } from 'generated/fetch';

import { render, screen } from '@testing-library/react';

import {
  ConfirmDeleteModal,
  ConfirmDeleteModalProps,
  ConfirmDeleteModalState,
} from './confirm-delete-modal';

describe('ConfirmDeleteModalComponent', () => {
  let props: ConfirmDeleteModalProps;

  const component = () => {
    return render(<ConfirmDeleteModal {...props} />);
  };

  beforeEach(() => {
    props = {
      resourceType: ResourceType.NOTEBOOK,
      resourceName: 'testResource',
      receiveDelete: () => {},
      closeFunction: () => {},
    };
  });

  it('should render', () => {
    const { container } = component();
    expect(screen.getByText(/are you sure you want to delete /i)).toBeTruthy();
  });
});
