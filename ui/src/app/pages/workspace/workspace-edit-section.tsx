import * as React from 'react';

import { FlexRow } from 'app/components/flex';
import { InfoIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { PubliclyDisplayed } from 'app/icons/publicly-displayed-icon';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

export const styles = reactStyles({
  header: {
    fontWeight: 600,
    lineHeight: '24px',
    color: colors.primary,
  },
  infoIcon: {
    height: '16px',
    marginLeft: '0.3rem',
    width: '16px',
  },
  requiredText: {
    fontSize: '13px',
    fontStyle: 'italic',
    fontWeight: 400,
    color: colors.primary,
    marginLeft: '0.3rem',
  },
  text: {
    fontSize: '14px',
    color: colors.primary,
    fontWeight: 400,
    lineHeight: '24px',
  },
});

interface Props {
  children?: string | React.ReactNode;
  dataTestId?: string;
  description?: string | React.ReactNode;
  descriptionStyle?: React.CSSProperties;
  header: any;
  index?: string;
  indexStyle?: React.CSSProperties;
  indent?: boolean;
  largeHeader?: any;
  required?: boolean;
  tooltip?: React.ReactNode;
  subHeader?: string;
  style?: any;
  publiclyDisplayed?: boolean;
}

export const WorkspaceEditSection = (props: Props) => {
  return (
    <div
      data-test-id={props.dataTestId}
      key={props.header}
      style={{ ...props.style, marginBottom: '0.75rem' }}
    >
      <FlexRow
        style={{
          marginBottom: props.largeHeader ? 12 : 0,
          marginTop: props.largeHeader ? 12 : 24,
        }}
      >
        {props.index && (
          <FlexRow
            style={{ ...styles.header, fontSize: props.largeHeader ? 18 : 14 }}
          >
            <div style={{ marginRight: '0.6rem', ...props.indexStyle }}>
              {props.index}
            </div>
            <div
              style={{
                ...styles.header,
                fontSize: props.largeHeader ? 18 : 14,
              }}
            >
              {props.header}
            </div>
            {props.publiclyDisplayed && (
              <PubliclyDisplayed
                style={{ marginLeft: '2.25rem', marginTop: '-0.15rem' }}
              />
            )}
          </FlexRow>
        )}
        {!props.index && (
          <div
            style={{ ...styles.header, fontSize: props.largeHeader ? 18 : 14 }}
          >
            {props.header}
          </div>
        )}
        {props.required && <div style={styles.requiredText}>(Required)</div>}
        {props.tooltip && (
          <TooltipTrigger content={props.tooltip}>
            <InfoIcon style={{ ...styles.infoIcon, marginTop: '0.3rem' }} />
          </TooltipTrigger>
        )}
      </FlexRow>
      {props.subHeader && (
        <div style={{ ...styles.header, color: colors.primary, fontSize: 14 }}>
          {props.subHeader}
        </div>
      )}
      <div
        style={{
          ...styles.text,
          marginLeft: '1.35rem',
          ...props.descriptionStyle,
        }}
      >
        {props.description}
      </div>
      <div
        style={{
          marginTop: '0.75rem',
          marginLeft: props.indent ? '1.35rem' : '0rem',
        }}
      >
        {props.children}
      </div>
    </div>
  );
};
