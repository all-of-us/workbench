import '@testing-library/jest-dom';

import * as React from 'react';
import { mockNavigate } from 'setupTests';

import { FileDetail } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';

import { FilenameCell } from './filename-cell';

let storageBucketPath: string,
  notebookFilename: string,
  notebookFile: FileDetail,
  accessReason: string,
  workspaceNamespace: string;

beforeEach(() => {
  storageBucketPath = 'gs://testbucket';
  notebookFilename = 'notes.ipynb';
  notebookFile = {
    name: notebookFilename,
    path: `${storageBucketPath}/notebooks/${notebookFilename}`,
    lastModifiedTime: new Date().valueOf(),
    sizeInBytes: 1e6, // 1 MB
  };
  accessReason = 'no reason, just being nosy';
  workspaceNamespace = 'some fake value';
});

afterEach(() => {
  jest.clearAllMocks();
});

describe(FilenameCell.name, () => {
  it.each(['notes.ipynb', 'data.sas', 'myAnalysis.Rmd', 'scripts.R'])(
    `allows preview for the file %s`,
    async (filename: string) => {
      const user = userEvent.setup();

      notebookFilename = filename;
      notebookFile = {
        name: notebookFilename,
        path: `${storageBucketPath}/notebooks/${notebookFilename}`,
        lastModifiedTime: new Date().valueOf(),
        sizeInBytes: 1e6, // 1 MB
      };

      const { container } = render(
        <FilenameCell
          {...{ accessReason, workspaceNamespace, storageBucketPath }}
          file={notebookFile}
        />
      );
      expect(container).toBeInTheDocument();
      expect(screen.getByText(notebookFilename)).toBeInTheDocument();

      const previewButton = screen.getByRole('button', { name: 'Preview' });
      expect(previewButton).toBeInTheDocument();
      expectButtonElementEnabled(previewButton);

      const expectedNavigation = [
        'admin',
        'workspaces',
        workspaceNamespace,
        notebookFilename,
      ];
      await waitFor(async () => {
        await user.click(previewButton);
        expect(mockNavigate).toHaveBeenCalledWith(expectedNavigation, {
          queryParams: { accessReason },
        });
      });
    }
  );

  it.each([
    ['the notebook is too large', () => (notebookFile.sizeInBytes = 10e6)],
    ['the access reason is empty', () => (accessReason = '')],
  ])(`does not enable the Preview button when %s`, async (_, setup) => {
    setup();

    const { container } = render(
      <FilenameCell
        {...{ accessReason, workspaceNamespace, storageBucketPath }}
        file={notebookFile}
      />
    );
    expect(container).toBeInTheDocument();
    expect(screen.getByText(notebookFilename)).toBeInTheDocument();

    const previewButton = screen.getByRole('button');
    expect(previewButton).toBeInTheDocument();
    expectButtonElementDisabled(previewButton);
  });

  test.each([
    '/',
    '/siblingdir/',
    '/notebooks/subdir/',
    '/notebooks/sub/sub/subdir/',
  ])(
    'does not show a Preview button when the notebook is in a nonstandard location (%s)',
    async (altPath) => {
      const path = `${storageBucketPath}/${altPath}/${notebookFilename}`;
      const { container } = render(
        <FilenameCell
          {...{ accessReason, workspaceNamespace, storageBucketPath }}
          file={{ ...notebookFile, path }}
        />
      );
      expect(container).toBeInTheDocument();
      expect(screen.getByText(notebookFilename)).toBeInTheDocument();

      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    }
  );

  it('does not show a Preview button when the file is not a notebook', async () => {
    const textFilename = 'myfile.txt';
    const textFile: FileDetail = {
      name: textFilename,
      path: `${storageBucketPath}/notebooks/${textFilename}`,
      lastModifiedTime: new Date().valueOf(),
      sizeInBytes: 1e6, // 1 MB
    };

    const { container } = render(
      <FilenameCell
        {...{ accessReason, workspaceNamespace, storageBucketPath }}
        file={textFile}
      />
    );
    expect(container).toBeInTheDocument();
    expect(screen.getByText(textFilename)).toBeInTheDocument();

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
