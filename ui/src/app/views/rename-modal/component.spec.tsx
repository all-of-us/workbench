import {mount} from 'enzyme';
import * as React from 'react';

import {RenameModal} from './component';

describe('RenameModal', () => {
  const existingNames = [];
  it('should render', () => {
    const wrapper = mount(<RenameModal
      onRename={(newName) => {}}
      type='Notebook' onCancel={() => {}}
      oldName=''
      existingNames={existingNames}
      nameFormat = {(name) => {}}/>
  );
    expect(wrapper.exists()).toBeTruthy();
  });
});
