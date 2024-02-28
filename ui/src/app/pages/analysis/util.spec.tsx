import { NotebooksApi } from 'generated/fetch';

import {
  notebooksApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';

import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';

import {
  appendAnalysisFileSuffixByOldName,
  getExistingJupyterNotebookNames,
} from './util';

describe(appendAnalysisFileSuffixByOldName.name, () => {
  it('should append Rmd if old file is Rmd file', () => {
    expect(appendAnalysisFileSuffixByOldName('test', 'old.Rmd')).toEqual(
      'test.Rmd'
    );
  });

  it('should append ipynb if old file is ipynb file', () => {
    expect(appendAnalysisFileSuffixByOldName('test', 'old.ipynb')).toEqual(
      'test.ipynb'
    );
  });

  it('should append sas if old file is sas file', () => {
    expect(appendAnalysisFileSuffixByOldName('test', 'old.sas')).toEqual(
      'test.sas'
    );
  });

  it('should not append anything if old file neither ipynb nor Rmd file', () => {
    expect(appendAnalysisFileSuffixByOldName('test', 'old.random')).toEqual(
      'test'
    );
  });
});

describe(getExistingJupyterNotebookNames.name, () => {
  const testWorkspace = workspaceStubs[0];

  const mockListCall = (fileNames: string[]) =>
    jest.spyOn(notebooksApi(), 'getNoteBookList').mockImplementation(
      (): Promise<any> =>
        Promise.resolve(
          fileNames.map((name) => ({
            name,
            path: 'test',
            lastModifiedTime: 1,
          }))
        )
    );

  beforeEach(async () =>
    registerApiClient(NotebooksApi, new NotebooksApiStub())
  );

  it('should return an empty list if the workspace has no notebooks', () => {
    mockListCall([]);
    expect(getExistingJupyterNotebookNames(testWorkspace)).resolves.toEqual([]);
  });

  it('should return a list of names of Jupyter notebooks only', () => {
    mockListCall([
      'Jupyter1.ipynb',
      'Sassy.sas',
      'arrrrr.Rmd',
      'yar.R',
      'Jupyter2.ipynb',
    ]);
    const expected = ['Jupyter1', 'Jupyter2'];
    expect(getExistingJupyterNotebookNames(testWorkspace)).resolves.toEqual(
      expected
    );
  });
});
