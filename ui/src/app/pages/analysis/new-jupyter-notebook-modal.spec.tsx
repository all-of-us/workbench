import '@testing-library/jest-dom';

import * as React from 'react';

import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { NewJupyterNotebookModal } from './new-jupyter-notebook-modal';

describe('NewNotebookModal', () => {
  it('should show error if new name already exists', async () => {
    const { getByTestId } = render(
      <NewJupyterNotebookModal
        onClose={() => {}}
        workspace={{ displayName: 'a' }}
        existingNameList={['123.ipynb']}
      />
    );
    fireEvent.change(getByTestId('create-jupyter-new-name-input'), {
      target: { value: '123' },
    });
    await waitFor(() =>
      expect(screen.getByText(/name already exists/i)).toBeInTheDocument()
    );
  });
});
