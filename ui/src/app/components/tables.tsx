import * as React from 'react';

import { FlexColumn, FlexRow } from 'app/components/flex';
import colors from 'app/styles/colors';

const styles = {
  paddedTable: {
    color: colors.primary,
    fontSize: 13,
    lineHeight: '24px',
    background: colors.white,
    padding: '0.3rem 0.75rem',
    display: 'flex',
  },
};

export const PaddedTableCell = ({
  left,
  content,
  leftWidth = '50%',
  rightWidth = '50%',
}) => {
  return (
    <div
      style={{
        padding: '.3rem',
        width: left ? leftWidth : rightWidth,
        display: 'flex',
      }}
    >
      <div style={{ ...styles.paddedTable, width: '100%' }}>{content}</div>
    </div>
  );
};

export const TwoColPaddedTable = ({
  style = {},
  header = false,
  headerLeft = '',
  headerRight = '',
  cellWidth = { left: '50%', right: '50%' },
  contentLeft,
  contentRight,
}) => {
  return (
    <FlexColumn style={{ ...style }}>
      {header && (
        <FlexRow key='header' style={{ height: '100%' }}>
          <PaddedTableCell
            key='header_l'
            left={true}
            leftWidth={cellWidth.left}
            content={<strong>{headerLeft}</strong>}
          />
          <PaddedTableCell
            key='header_r'
            left={false}
            rightWidth={cellWidth.right}
            content={<strong>{headerRight}</strong>}
          />
        </FlexRow>
      )}
      {contentLeft.map((c, i) => (
        <FlexRow key={i}>
          <PaddedTableCell
            key={i + '_l'}
            left={true}
            content={c}
            leftWidth={cellWidth.left}
          />
          {contentRight.length >= i + 1 && (
            <PaddedTableCell
              key={i + '_r'}
              left={false}
              content={contentRight[i]}
              rightWidth={cellWidth.right}
            />
          )}
        </FlexRow>
      ))}
    </FlexColumn>
  );
};
