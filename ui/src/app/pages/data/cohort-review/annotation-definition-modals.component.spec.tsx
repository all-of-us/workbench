import '@testing-library/jest-dom';

import * as React from 'react';

import { screen } from '@testing-library/react';

import { renderWithRouter } from 'testing/react-test-helpers';
import { cohortAnnotationDefinitionStub } from 'testing/stubs/cohort-annotation-definition-service-stub';

import {
  AddAnnotationDefinitionModal,
  EditAnnotationDefinitionsModal,
} from './annotation-definition-modals.component';

describe('AddAnnotationDefinitionModal', () => {
  it('should render', () => {
    renderWithRouter(
      <AddAnnotationDefinitionModal
        annotationDefinitions={[cohortAnnotationDefinitionStub]}
        onCancel={() => {}}
        onCreate={() => {}}
      />
    );
    expect(
      screen.getByText('Create a Review-Wide Annotation Field')
    ).toBeInTheDocument();
  });
});

describe('EditAnnotationDefinitionsModal', () => {
  it('should render', () => {
    renderWithRouter(
      <EditAnnotationDefinitionsModal
        annotationDefinitions={[cohortAnnotationDefinitionStub]}
        setAnnotationDefinitions={() => {}}
        onClose={() => {}}
      />
    );
    expect(
      screen.getByText('Edit or Delete Review-Wide Annotation Fields')
    ).toBeInTheDocument();
  });
});
