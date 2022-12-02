import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import jupyterLogo from 'assets/images/Jupyter.png';
import jupyterIcon from 'assets/images/Jupyter-icon.png';
import rStudioLogo from 'assets/images/RStudio.png';
import rStudioIcon from 'assets/images/RStudio-icon.png';

export const buttonStyles = reactStyles({
  button: {
    opacity: 1,
    boxSizing: 'border-box',
    height: 69,
    width: 108,
    border: '1px solid #979797',
    borderRadius: 9,
    alignItems: 'center',
  },
  disabledButton: {
    opacity: 0.46,
    cursor: 'not-allowed',
    boxSizing: 'border-box',
    height: 69,
    width: 108,
    border: '1px solid #979797',
    borderRadius: 9,
    alignItems: 'center',
  },
  enabledTrashButton: {
    alignSelf: 'center',
    marginLeft: 'auto',
    marginRight: '1em',
  },
  disabledTrashButton: {
    alignSelf: 'center',
    marginLeft: 'auto',
    marginRight: '1em',
    color: colors.disabled,
    cursor: 'not-allowed',
  },
  buttonText: {
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontWeight: 'bold',
    letterSpacing: 0,
    lineHeight: '15px',
  },
  disabledButtonText: {
    color: colors.secondary,
    fontFamily: 'Montserrat',
    fontWeight: 'bold',
    letterSpacing: 0,
    lineHeight: '15px',
  },
  buttonIcon: {
    height: 23,
    padding: '0.7em',
  },
});

// Eventually we will need to align this with the API's AppType
export enum UIAppType {
  JUPYTER = 'Jupyter',
  RSTUDIO = 'RStudio',
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
];
