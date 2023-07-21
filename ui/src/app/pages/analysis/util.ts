import { FileDetail } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { notebooksApi } from 'app/services/swagger-fetch-clients';
import { cond } from 'app/utils';
import {
  JUPYTER_FILE_EXT,
  R_FILE_EXT,
  RMD_FILE_EXT,
} from 'app/utils/constants';

// todo generically drop suffix when previewing?
export function dropJupyterNotebookFileSuffix(filename: string) {
  if (filename?.endsWith(JUPYTER_FILE_EXT)) {
    return filename.substring(0, filename.length - JUPYTER_FILE_EXT.length);
  }

  return filename;
}

export function appendJupyterNotebookFileSuffix(filename: string) {
  if (filename && !filename.endsWith(JUPYTER_FILE_EXT)) {
    return filename + JUPYTER_FILE_EXT;
  }

  return filename;
}

export function appendRstudioNotebookFileSuffix(filename: string) {
  if (filename && !filename.endsWith(RMD_FILE_EXT)) {
    return filename + RMD_FILE_EXT;
  }

  return filename;
}

export function appendRNotebookFileSuffix(filename: string) {
  if (filename && !filename.endsWith(R_FILE_EXT)) {
    return filename + R_FILE_EXT;
  }

  return filename;
}

export function appendNotebookFileSuffixByOldName(
  filename: string,
  oldFileName: string
) {
  return cond(
    [
      oldFileName.endsWith(JUPYTER_FILE_EXT),
      () => appendJupyterNotebookFileSuffix(filename),
    ],
    [
      oldFileName.endsWith(RMD_FILE_EXT),
      () => appendRstudioNotebookFileSuffix(filename),
    ],
    [
      oldFileName.endsWith(R_FILE_EXT),
      () => appendRNotebookFileSuffix(filename),
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
    extension: JUPYTER_FILE_EXT,
    appType: UIAppType.JUPYTER,
    canPlayground: true,
  },
  {
    extension: RMD_FILE_EXT,
    appType: UIAppType.RSTUDIO,
    canPlayground: false,
  },
  {
    extension: R_FILE_EXT,
    appType: UIAppType.RSTUDIO,
    canPlayground: false,
  },
];

export const getAppInfoFromFileName = (name: string) => {
  return appsExtensionMap.find((app) => name.endsWith(app.extension));
};
