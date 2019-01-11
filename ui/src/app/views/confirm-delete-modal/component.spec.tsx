import { shallow } from 'enzyme';
import * as React from 'react';

import {ConfirmDeleteModal, ConfirmDeleteModalProps, ConfirmDeleteModalState} from '../confirm-delete-modal/component';

describe('ConfirmDeleteModalComponent', () => {
  let props: ConfirmDeleteModalProps;

  const component = () => {
    return shallow<ConfirmDeleteModal, ConfirmDeleteModalProps, ConfirmDeleteModalState>
    (<ConfirmDeleteModal {...props}/>);
  };

  beforeEach(() => {
    props = {
      deleting: true,
      resourceType: 'test',
      resource: {name: 'testResource'},
      receiveDelete: () => {},
      closeFunction: () => {}
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

};
