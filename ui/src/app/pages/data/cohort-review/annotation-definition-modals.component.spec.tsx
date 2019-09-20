import {mount} from 'enzyme';
import * as React from 'react';

import {cohortAnnotationDefinitionStub} from 'testing/stubs/cohort-annotation-definition-service-stub';

import {AddAnnotationDefinitionModal, EditAnnotationDefinitionsModal} from './annotation-definition-modals.component';

describe('AddAnnotationDefinitionModal', () => {
  it('should render', () => {
    const wrapper = mount(<AddAnnotationDefinitionModal
      annotationDefinitions={[cohortAnnotationDefinitionStub]}
      onCancel={() => {}}
      onCreate={() => {}}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});

describe('EditAnnotationDefinitionsModal', () => {
  it('should render', () => {
    const wrapper = mount(<EditAnnotationDefinitionsModal
      annotationDefinitions={[cohortAnnotationDefinitionStub]}
      setAnnotationDefinitions={() => {}}
      onClose={() => {}}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
