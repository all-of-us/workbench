import * as React from 'react';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import headerImage from 'assets/images/all-of-us-logo.svg';

const styles = reactStyles({
  content: {
    color: colors.primary,
    margin: '1rem',
  },
});

export const PUBLIC_HEADER_IMAGE = headerImage;

/**
 * A layout component suitable for display public-facing content, such as static
 * content and error pages.
 */
export const PublicLayout = ({ contentStyle = {}, children }) => {
  return (
    <React.Fragment>
      <div
        style={{
          width: '100%',
          height: '3.5rem',
          borderBottom: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
        }}
      >
        <a href='/'>
          <img
            style={{ height: '1.75rem', marginLeft: '1rem', marginTop: '1rem' }}
            src={PUBLIC_HEADER_IMAGE}
          />
        </a>
      </div>
      <div style={{ ...styles.content, ...contentStyle }}>{children}</div>
    </React.Fragment>
  );
};
