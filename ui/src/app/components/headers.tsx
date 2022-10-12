import * as React from 'react';

import { environment } from 'environments/environment';
import colors from 'app/styles/colors';
import { reactStyles, withStyle } from 'app/utils/index';
import logo from 'assets/images/all-of-us-logo.svg';

export const styles = reactStyles({
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
  semiBold: {
    fontWeight: 600,
  },
  h4: {
    fontWeight: 300,
    color: colors.primary,
  },
  formLabel: {
    color: colors.primary,
    fontWeight: 600,
    marginTop: '0.5rem',
    marginBottom: '0.125rem',
  },
  listPageHeader: {
    color: colors.primary,
    fontSize: '20px',
    lineHeight: '24px',
    fontWeight: 600,
    marginTop: 0,
    paddingTop: '0.5rem',
  },
  pageHeader: {
    color: colors.primary,
    fontSize: '28px',
    fontWeight: 400,
  },
  headerImage: {
    height: '57px',
    width: '155px',
    marginLeft: '1rem',
  },
  displayTag: {
    marginLeft: '1rem',
    height: '12px',
    width: '155px',
    borderRadius: '2px',
    backgroundColor: colors.primary,
    color: colors.white,
    fontFamily: 'Montserrat',
    fontSize: '8px',
    lineHeight: '12px',
    textAlign: 'center',
  },
});

export const BolderHeader = withStyle(styles.h1)('h1');
export const BoldHeader = withStyle(styles.h2)('h2');
export const Header = withStyle(styles.h3)('h3');
export const SemiBoldHeader = withStyle({ ...styles.h3, ...styles.semiBold })(
  'h3'
);
export const SmallHeader = withStyle(styles.h4)('h4');
export const ListPageHeader = withStyle(styles.listPageHeader)('h3');
export const PageHeader = withStyle(styles.pageHeader)('h2');

export const AouHeaderWithDisplayTag = () => (
  <div>
    <a href='/'>
      <img style={styles.headerImage} src={logo} alt='all of us logo' />
    </a>
    {environment.shouldShowDisplayTag && (
      <div style={styles.displayTag}>{environment.displayTag}</div>
    )}
  </div>
);
