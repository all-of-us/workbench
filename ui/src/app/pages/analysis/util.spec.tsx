import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { WorkspaceAccessLevel } from 'generated/fetch';

import { NotebookResourceCard } from 'app/pages/analysis/notebook-resource-card';

import { appendNotebookFileSuffixByOldName } from './util';

describe('NotebookUtil', () => {
  it('should append Rmd if old file is Rmd file', () => {
    expect(appendNotebookFileSuffixByOldName('test', 'old.Rmd')).toEqual(
      'test.Rmd'
    );
  });

  it('should append ipynb if old file is ipynb file', () => {
    expect(appendNotebookFileSuffixByOldName('test', 'old.ipynb')).toEqual(
      'test.ipynb'
    );
  });
});
