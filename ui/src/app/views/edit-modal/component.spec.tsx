import { shallow } from 'enzyme';
import * as React from 'react';

import {EditModal, EditModalProps, EditModalState} from '../edit-modal/component';

describe('ConfirmDeleteModalComponent', () => {
  let props: EditModalProps;

  const component = () => {
    return shallow<EditModal, EditModalProps, EditModalState>
    (<EditModal {...props}/>);
  };

  beforeEach(() => {
    props = {
      editing: true,
      resource: {},
      onEdit: () => {},
      onCancel: () => {}
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

});
