import React, { CSSProperties } from 'react';
import { Link } from 'react-router-dom';

import { WorkspaceResource } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { stringifyUrl } from 'app/utils/navigation';
import { getResourceUrl, isNotebook } from 'app/utils/resources';

const styles = reactStyles({
  resourceName: {
    fontSize: '18px',
    fontWeight: 500,
    lineHeight: '22px',
    color: colors.accent,
    cursor: 'pointer',
    wordBreak: 'break-all',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
    display: '-webkit-box',
    WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical',
    textDecoration: 'none',
  },
});

interface Props {
  resource: WorkspaceResource;
  linkTestId?: string;
  style?: CSSProperties;
  children: string | React.ReactNode;
}
export const ResourceNavigation = (props: Props) => {
  const {
    resource,
    resource: { adminLocked },
    linkTestId,
    style = styles.resourceName,
    children,
  } = props;
  const url = stringifyUrl(getResourceUrl(resource));

  function onNavigate() {
    if (isNotebook(resource)) {
      AnalyticsTracker.Notebooks.Preview();
    }
  }

  return (
    <div>
      {adminLocked ? (
        <div>{children}</div>
      ) : (
        <Clickable>
          <Link
            to={url}
            style={style}
            data-test-id={linkTestId}
            onClick={() => onNavigate()}
          >
            {children}
          </Link>
        </Clickable>
      )}
    </div>
  );
};
