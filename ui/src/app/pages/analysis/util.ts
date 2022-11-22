import { FileDetail } from 'generated/fetch';

import { notebooksApi } from 'app/services/swagger-fetch-clients';

const notebookExtension = '.ipynb';

export function dropNotebookFileSuffix(filename: string) {
  if (filename?.endsWith(notebookExtension)) {
    filename = filename.substring(0, filename.length - 6);
  }

  return filename;
}

export function appendNotebookFileSuffix(filename: string) {
  if (filename && !filename.endsWith(notebookExtension)) {
    filename = filename + notebookExtension;
  }

  return filename;
}

export const getExistingNotebooks = (workspace): Promise<FileDetail[]> => {
  const { namespace, id } = workspace;
  return notebooksApi().getNoteBookList(namespace, id);
};

export const getExistingNotebookNames = async (
  workspace
): Promise<string[]> => {
  const notebooks = await getExistingNotebooks(workspace);
  return notebooks.map((fd) => dropNotebookFileSuffix(fd.name));
};
