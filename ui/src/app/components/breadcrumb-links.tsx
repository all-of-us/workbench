import * as React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import * as fp from 'lodash/fp';

import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  firstLink: {
    color: colors.accent,
    textDecoration: 'none',
  },
  lastLink: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: '1rem',
    textDecoration: 'none',
  },
});

export class BreadcrumbData {
  label: string;
  url: string;

  constructor(label: string, url: string) {
    this.label = label;
    this.url = url;
  }
}

const first = (trail: BreadcrumbData[]): BreadcrumbData[] =>
  fp.dropRight(1, trail);
const last = (trail: BreadcrumbData[]): BreadcrumbData => fp.last(trail);

interface Props {
  trail: BreadcrumbData[];
}
export const BreadcrumbLinks = (props: Props) => {
  const lastBreadcrumb = last(props.trail);
  return (
    <div
      style={{
        marginLeft: '3.25rem',
        display: 'inline-block',
      }}
    >
      {first(props.trail).map(({ label, url }, i) => {
        return (
          <React.Fragment key={i}>
            <RouterLink to={url} style={styles.firstLink}>
              {label}
            </RouterLink>
            <span
              style={{
                color: colors.primary,
              }}
            >
              {' '}
              &gt;{' '}
            </span>
          </React.Fragment>
        );
      })}
      {lastBreadcrumb && (
        <div>
          <RouterLink to={lastBreadcrumb.url} style={styles.lastLink}>
            {lastBreadcrumb.label}
          </RouterLink>
        </div>
      )}
    </div>
  );
};
