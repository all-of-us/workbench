import * as React from 'react';

import { Button } from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import { reactStyles } from 'app/utils';


const styles = reactStyles({
  container: {
    backgroundColor: colors.white,
    color: colors.primary,
    height: '225px',
    width: '220px',
    borderRadius: 8,
    marginBottom: '10px',
    padding: 14,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`
  },
  title: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 600,
    marginBottom: 6
  },
  button: {
    height: '38px'
  },
  successButton: {
    backgroundColor: colors.success,
    cursor: 'default'
  }
});

interface Props {
  containerStylesOverride?: React.CSSProperties;
  title: string | React.ReactNode;
  wasBypassed: boolean;
  isComplete: boolean;
  incompleteButtonText: string;
  completedButtonText: string;
  completeStep: Function;
  childrenStyle?: React.CSSProperties;
  content?: JSX.Element | JSX.Element[]; // | React.ReactChildren;
}

const ProfileRegistrationStepStatus: React.FunctionComponent<Props> =
  (props) => {
    const {
      title,
      wasBypassed,
      isComplete,
      incompleteButtonText,
      completedButtonText,
      completeStep,
      childrenStyle,
      children,
      content
    } = props;

    return (
      <FlexColumn style={{...styles.container, ...props.containerStylesOverride}}>
        <div style={styles.title}>
          { title }
        </div>
        <FlexColumn style={{
          justifyContent: isComplete && children ? 'flex-end' : 'space-between', 
          flex: '1 1 auto',
          alignItems: 'baseline'
          }}>
          {isComplete && <div style={childrenStyle}>{ content }</div>}
          {children}
          {isComplete && <Button disabled={true} 
                                data-test-id='completed-button' 
                                style={{...styles.button, backgroundColor: colors.success, width: 'max-content', cursor: 'default'}}>
              <ClrIcon shape='check' style={{marginRight: '0.3rem'}}/>{wasBypassed ? 'Bypassed' : completedButtonText}
            </Button>
          }
          {!isComplete && <Button type='purplePrimary' style={ styles.button } onClick={ completeStep }>{ incompleteButtonText }</Button>}
        </FlexColumn>
      </FlexColumn>
    );
  };

export {
  ProfileRegistrationStepStatus,
  Props as ProfileRegistrationStepStatusProps
};