import { FileDetail, Workspace } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { notebooksApi } from 'app/services/swagger-fetch-clients';
import {
  JUPYTER_FILE_EXT,
  R_SCRIPT_EXT,
  RMD_FILE_EXT,
  SAS_FILE_EXT,
} from 'app/utils/constants';
import { UIAppType } from 'app/utils/user-apps-utils';

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

export function appendRStudioNotebookFileSuffix(filename: string) {
  if (filename && !filename.endsWith(RMD_FILE_EXT)) {
    return filename + RMD_FILE_EXT;
  }

  return filename;
}

export function appendRScriptSuffix(filename: string) {
  if (filename && !filename.endsWith(R_SCRIPT_EXT)) {
    return filename + R_SCRIPT_EXT;
  }

  return filename;
}

export function appendSasSuffix(filename: string) {
  if (filename && !filename.endsWith(SAS_FILE_EXT)) {
    return filename + SAS_FILE_EXT;
  }

  return filename;
}

export function appendAnalysisFileSuffixByOldName(
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
      () => appendRStudioNotebookFileSuffix(filename),
    ],
    [oldFileName.endsWith(R_SCRIPT_EXT), () => appendRScriptSuffix(filename)],
    [oldFileName.endsWith(SAS_FILE_EXT), () => appendSasSuffix(filename)],
    () => filename
  );
}

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
    extension: R_SCRIPT_EXT,
    appType: UIAppType.RSTUDIO,
    canPlayground: false,
  },
  {
    extension: SAS_FILE_EXT,
    appType: UIAppType.SAS,
    canPlayground: false,
  },
];

export const getAppInfoFromFileName = (name: string) => {
  return appsExtensionMap.find((app) => name.endsWith(app.extension));
};

export const listNotebooks = (workspace: Workspace): Promise<FileDetail[]> => {
  const { namespace, id } = workspace;
  return notebooksApi().getNoteBookList(namespace, id);
};

export const getExistingJupyterNotebookNames = async (
  workspace: Workspace
): Promise<string[]> => {
  const notebooks = await listNotebooks(workspace);
  return notebooks
    .filter(
      (fd: FileDetail) =>
        getAppInfoFromFileName(fd.name).appType === UIAppType.JUPYTER
    )
    .map((fd: FileDetail) => dropJupyterNotebookFileSuffix(fd.name));
};

export const parseDirectory = (
  file: FileDetail,
  storageBucketPath: string
): string => {
  const prefixLength = storageBucketPath.length;
  const suffixPos = file.path.lastIndexOf(file.name);
  const start = prefixLength + 1; // slash after bucket name
  const end = suffixPos - 1; // slash before filename

  return file.path.substring(start, end);
};
