import { shallow } from 'enzyme';
import * as React from 'react';
import {RecentResource} from 'generated';
import {EditModal, EditModalProps, EditModalState} from '../edit-modal/component';

describe('ConfirmDeleteModalComponent', () => {
  let props: EditModalProps;

  const component = () => {
    return shallow<EditModal, EditModalProps, EditModalState>
    (<EditModal {...props}/>);
  };

  beforeEach(() => {
    const resource: RecentResource = {
      workspaceId: 1,
      conceptSet: {name: 'test'}
    };

    props = {
      resource: resource,
      onEdit: () => {},
      onCancel: () => {}
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

});
