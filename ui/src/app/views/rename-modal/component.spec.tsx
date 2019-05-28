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

  it('should display description if props displayDescription is set to true', () => {
    const wrapper = mount(<RenameModal
        onRename={(newName) => {}}
        type='Notebook' onCancel={() => {}}
        oldName=''
        existingNames={existingNames}
        nameFormat = {(name) => {}}
        displayDescription={true}/>
    );
    expect(wrapper.exists()).toBeTruthy();
    expect(wrapper.find('[data-test-id="descriptionLabel"]')).toBeTruthy();
  });

});
