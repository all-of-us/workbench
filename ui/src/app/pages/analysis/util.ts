import { FileDetail } from 'generated/fetch';

import { notebooksApi } from 'app/services/swagger-fetch-clients';
import { cond } from 'app/utils';

const jupyterNotebookExtension = '.ipynb';
const rstudioNotebookExtension = '.Rmd';

export function dropJupyterNotebookFileSuffix(filename: string) {
  if (filename?.endsWith(jupyterNotebookExtension)) {
    return filename.substring(
      0,
      filename.length - jupyterNotebookExtension.length
    );
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

export function appendNotebookFileSuffixByOldName(
  filename: string,
  oldFileName: string
) {
  cond(
    [
      oldFileName.endsWith(jupyterNotebookExtension),
      () => appendJupyterNotebookFileSuffix(filename),
    ],
    [
      oldFileName.endsWith(rstudioNotebookExtension),
      () => appendRstudioNotebookFileSuffix(filename),
    ],
    () => filename
  );
}

export const listNotebooks = (workspace): Promise<FileDetail[]> => {
  const { namespace, id } = workspace;
  return notebooksApi().getNoteBookList(namespace, id);
};

export const getExistingNotebookNames = async (
  workspace
): Promise<string[]> => {
  const notebooks = await listNotebooks(workspace);
  return notebooks.map((fd) => dropJupyterNotebookFileSuffix(fd.name));
};
