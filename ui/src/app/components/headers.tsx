
import colors from 'app/styles/colors';
import {withStyle} from 'app/utils/index';


export const styles = {
  h1: {
    color: colors.primary,
    fontWeight: 500,
    fontSize: '1.6rem',
  },
  h2: {
    color: colors.primary,
    fontWeight: 800,
    fontSize: '1.25rem',
  },
  h3: {
    color: colors.primary,
    fontWeight: 800,
    fontSize: '1rem',
  },
  lessBold: {
    fontWeight: 600
  },
  h4: {
    fontWeight: 300,
    color: colors.primary,
  },
  formLabel: {
    color: colors.primary,
    fontWeight: 600,
    marginTop: '0.5rem',
    marginBottom: '0.125rem'
  },
  listPageHeader: {
    color: colors.primary,
    fontSize: '20px',
    lineHeight: '24px',
    fontWeight: 600,
    marginTop: 0,
    paddingTop: '0.5rem'
  }
};

export const BolderHeader = withStyle(styles.h1)('h1');
export const BoldHeader = withStyle(styles.h2)('h2');
export const Header = withStyle(styles.h3)('h3');
export const LessBoldHeader = withStyle({...styles.h3, ...styles.lessBold})('h3');
export const SmallHeader = withStyle(styles.h4)('h4');
export const ListPageHeader = withStyle(styles.listPageHeader)('h3');
