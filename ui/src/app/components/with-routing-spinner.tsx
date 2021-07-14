import colors from 'app/styles/colors';
import {withSpinnerOverlay} from './with-spinner-overlay';

export const withRoutingSpinner = withSpinnerOverlay(
  true,
    {dark: true, opacity: 0.8, overrideStylesOverlay: {backgroundColor: colors.black}});
