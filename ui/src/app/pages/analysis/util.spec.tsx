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

  it('should not append anything if old file neither ipynb nor Rmd file', () => {
    expect(appendNotebookFileSuffixByOldName('test', 'old.random')).toEqual(
      'test'
    );
  });
});
