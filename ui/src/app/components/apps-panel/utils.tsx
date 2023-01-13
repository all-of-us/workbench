import { AppType, CreateAppRequest } from '../../../generated/fetch';
import cromwellLogo from 'assets/images/Cromwell.png';
import cromwellIcon from 'assets/images/Cromwell-icon.png';
import jupyterLogo from 'assets/images/Jupyter.png';
import jupyterIcon from 'assets/images/Jupyter-icon.png';
import rStudioLogo from 'assets/images/RStudio.png';
import rStudioIcon from 'assets/images/RStudio-icon.png';

// Eventually we will need to align this with the API's AppType
export enum UIAppType {
  JUPYTER = 'Jupyter',
  RSTUDIO = 'RStudio',
  CROMWELL = 'Cromwell',
}

interface AppAssets {
  appType: UIAppType;
  logo: string;
  icon: string;
}
export const appAssets: AppAssets[] = [
  {
    appType: UIAppType.JUPYTER,
    logo: jupyterLogo,
    icon: jupyterIcon,
  },
  {
    appType: UIAppType.RSTUDIO,
    logo: rStudioLogo,
    icon: rStudioIcon,
  },
  {
    appType: UIAppType.CROMWELL,
    logo: cromwellLogo,
    icon: cromwellIcon,
  },
];

export const defaultCromwellConfig: CreateAppRequest = {
  appType: AppType.CROMWELL,
  autopauseThreshold: 0, // number
  kubernetesRuntimeConfig: {
    numNodes: 0, // number
    machineType: null, // string
    autoscalingEnabled: false, // boolean
  },
  persistentDiskRequest: {
    name: null, // string
    size: 0, // number
    diskType: null, // Standard or Ssd
    labels: 0, // Map[String,String]
  },
};
