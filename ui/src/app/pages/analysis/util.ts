import { notebooksApi } from 'app/services/swagger-fetch-clients';

const notebookExtension = '.ipynb';

export function dropNotebookFileSuffix(filename: string) {
  if (filename.endsWith(notebookExtension)) {
    filename = filename.substring(0, filename.length - 6);
  }

  return filename;
}

export function appendNotebookFileSuffix(filename: string) {
  if (!filename.endsWith(notebookExtension)) {
    filename = filename + notebookExtension;
  }

  return filename;
}

export const getExistingNotebookNames = async (workspace) => {
  const { namespace, id } = workspace;
  const notebook = await notebooksApi().getNoteBookList(namespace, id);
  return notebook.map((fd) => dropNotebookFileSuffix(fd.name));
};
