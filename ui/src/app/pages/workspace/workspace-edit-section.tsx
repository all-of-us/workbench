import {FlexRow} from 'app/components/flex';
import {InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import * as React from 'react';

export const styles = reactStyles({
  header: {
    fontWeight: 600,
    lineHeight: '24px',
    color: colors.primary
  },
  infoIcon: {
    height: '16px',
    marginLeft: '0.2rem',
    width: '16px'
  },
  requiredText: {
    fontSize: '13px',
    fontStyle: 'italic',
    fontWeight: 400,
    color: colors.primary,
    marginLeft: '0.2rem'
  },
  text: {
    fontSize: '13px',
    color: colors.primary,
    fontWeight: 400,
    lineHeight: '24px'
  }
});

interface Props {
  children?: string | React.ReactNode;
  description?: string | React.ReactNode;
  header: any;
  index?: string;
  indent?: boolean;
  largeHeader?: any;
  required?: boolean;
  tooltip?: React.ReactNode;
  subHeader?: string;
  style?: any;
}

export const WorkspaceEditSection = (props: Props) => {
  return <div key={props.header} style={{...props.style, marginBottom: '0.5rem'}}>
    <FlexRow style={{marginBottom: (props.largeHeader ? 12 : 0),
      marginTop: (props.largeHeader ? 12 : 24)}}>
      {props.index && <FlexRow style={{...styles.header,
        fontSize: (props.largeHeader ? 20 : 16)}}>
        <div style={{marginRight: '0.4rem'}}>{props.index}</div>
        <div style={{...styles.header,
          fontSize: (props.largeHeader ? 20 : 16)}}>
          {props.header}
        </div>
      </FlexRow>}
      {!props.index &&
      <div style={{...styles.header,
        fontSize: (props.largeHeader ? 20 : 16)}}>
        {props.header}
      </div>
      }
      {props.required && <div style={styles.requiredText}>
        (Required)
      </div>
      }
      {props.tooltip && <TooltipTrigger content={props.tooltip}>
        <InfoIcon style={{...styles.infoIcon,  marginTop: '0.2rem'}}/>
      </TooltipTrigger>
      }
    </FlexRow>
    {props.subHeader && <div style={{...styles.header, color: colors.primary, fontSize: 14}}>
      {props.subHeader}
    </div>
    }
    <div style={{...styles.text, marginLeft: '0.9rem'}}>
      {props.description}
    </div>
    <div style={{marginTop: '0.5rem', marginLeft: (props.indent ? '0.9rem' : '0rem')}}>
      {props.children}
    </div>
  </div>;
};
