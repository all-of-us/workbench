import {EditModal, EditModalProps, EditModalState} from 'app/views/edit-modal';
import {shallow} from 'enzyme';
import {RecentResource} from 'generated/fetch';
import * as React from 'react';

describe('EditModalComponent', () => {
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
