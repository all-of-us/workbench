import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';

// Contains style definitions shared across multiple account-creation form steps.
export const commonStyles = reactStyles({
  asideContainer: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.85),
    borderRadius: 8,
    width: '18rem',
    padding: '0.5rem'
  },
  asideHeader: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: 16,
  },
  asideList: {
    display: 'flex',
    height: '100%',
    flexDirection: 'column',
    justifyContent: 'space-evenly'
  },
  asideText: {
    fontSize: 14,
    fontWeight: 400,
    color: colors.primary,
  },
});