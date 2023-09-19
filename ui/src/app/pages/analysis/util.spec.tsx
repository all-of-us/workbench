import { appendAnalysisFileSuffixByOldName } from './util';

describe('NotebookUtil', () => {
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
