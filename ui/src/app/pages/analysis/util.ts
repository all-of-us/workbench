import { FileDetail } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
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
  return cond(
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

const appsExtensionMap = [
  {
    extension: jupyterNotebookExtension,
    appType: UIAppType.JUPYTER,
    canPlayground: true,
  },
  {
    extension: rstudioNotebookExtension,
    appType: UIAppType.RSTUDIO,
    canPlayground: false,
  },
];

export const getAppInfoFromFileName = (name: string) => {
  return appsExtensionMap.find((app) => name.endsWith(app.extension));
};
