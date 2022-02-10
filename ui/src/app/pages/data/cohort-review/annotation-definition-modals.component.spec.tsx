import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { mount } from 'enzyme';

import { cohortAnnotationDefinitionStub } from 'testing/stubs/cohort-annotation-definition-service-stub';

import {
  AddAnnotationDefinitionModal,
  EditAnnotationDefinitionsModal,
} from './annotation-definition-modals.component';

describe('AddAnnotationDefinitionModal', () => {
  it('should render', () => {
    const wrapper = mount(
      <MemoryRouter>
        <AddAnnotationDefinitionModal
          annotationDefinitions={[cohortAnnotationDefinitionStub]}
          onCancel={() => {}}
          onCreate={() => {}}
        />
      </MemoryRouter>
    );
    expect(wrapper.exists()).toBeTruthy();
  });
});

describe('EditAnnotationDefinitionsModal', () => {
  it('should render', () => {
    const wrapper = mount(
      <MemoryRouter>
        <EditAnnotationDefinitionsModal
          annotationDefinitions={[cohortAnnotationDefinitionStub]}
          setAnnotationDefinitions={() => {}}
          onClose={() => {}}
        />
      </MemoryRouter>
    );
    expect(wrapper.exists()).toBeTruthy();
  });
});
