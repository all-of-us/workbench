import { FileDetail } from 'generated/fetch';

import { notebooksApi } from 'app/services/swagger-fetch-clients';

const jupyterNotebookExtension = '.ipynb';
const rstudioNotebookExtension = '.Rmd';

export function dropNotebookFileSuffix(filename: string) {
  if (filename?.endsWith(jupyterNotebookExtension)) {
    return filename.substring(0, filename.length - jupyterNotebookExtension.length);
  }

  return filename;
}

export function appendJupyterNotebookFileSuffix(filename: string) {
  if (filename && !filename.endsWith(jupyterNotebookExtension)) {
    return filename + jupyterNotebookExtension;
  }

  return filename;
}

export function appendRstudioNotebookFileSuffix(filename: string) {
  if (filename && !filename.endsWith(rstudioNotebookExtension)) {
    return filename + rstudioNotebookExtension;
  }

  return filename;
}

export function appendNotebookFileSuffixByOldName(filename: string, oldFileName: string) {
  if(oldFileName.endsWith(jupyterNotebookExtension)) {
    return appendJupyterNotebookFileSuffix(filename)
  } else {
    return appendRstudioNotebookFileSuffix(filename)
  }
}


export const listNotebooks = (workspace): Promise<FileDetail[]> => {
  const { namespace, id } = workspace;
  return notebooksApi().getNoteBookList(namespace, id);
};

export const getExistingNotebookNames = async (
  workspace
): Promise<string[]> => {
  const notebooks = await listNotebooks(workspace);
  return notebooks.map((fd) => dropNotebookFileSuffix(fd.name));
};
