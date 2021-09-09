import {shallow} from 'enzyme';
import {ResourceType} from 'generated/fetch';
import * as React from 'react';

import {ConfirmDeleteModal, ConfirmDeleteModalProps, ConfirmDeleteModalState} from './confirm-delete-modal';

describe('ConfirmDeleteModalComponent', () => {
  let props: ConfirmDeleteModalProps;

  const component = () => {
    return shallow<ConfirmDeleteModal, ConfirmDeleteModalProps, ConfirmDeleteModalState>
    (<ConfirmDeleteModal {...props}/>);
  };

  beforeEach(() => {
    props = {
      resourceType: ResourceType.NOTEBOOK,
      resourceName: 'testResource',
      receiveDelete: () => {},
      closeFunction: () => {}
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

});
