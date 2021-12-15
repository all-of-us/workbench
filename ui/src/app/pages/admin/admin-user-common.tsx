// this is a temporary file to assist with the migration from (class-based) AdminUser to (functional) AdminUserProfile
// for RW-7536

import {reactStyles} from 'app/utils';
import colors, {colorWithWhiteness} from 'app/styles/colors';

export const styles = reactStyles({
  semiBold: {
    fontWeight: 600
  },
  backgroundColorDark: {
    backgroundColor: colorWithWhiteness(colors.primary, .95)
  },
  textInput: {
    width: '17.5rem',
    opacity: '100%',
  },
  textInputContainer: {
    marginTop: '1rem'
  },
  fadeBox: {
    margin: 'auto',
    paddingTop: '1rem',
    width: '96.25%',
    minWidth: '1232px',
    color: colors.primary
  },
});
