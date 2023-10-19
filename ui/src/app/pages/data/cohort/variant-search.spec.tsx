import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { VariantSearch } from 'app/pages/data/cohort/variant-search';

describe('VariantSearch', () => {
  const component = () => {

    return mount(
      <MemoryRouter>
        <VariantSearch select={() => {}} selectedIds={[]} />
      </MemoryRouter>
    );
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
