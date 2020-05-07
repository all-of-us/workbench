import * as React from 'react';

import { Button } from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {environment} from 'environments/environment';

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
  },
  buttonContainerToDeleteWithEnableProfileCapsFeatures: {
    width: '50%',
    marginTop: 6
  },
  detailsContainerToDeleteWithEnableProfileCapsFeatures: {
    width: '50%',
    marginLeft: 40
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
  completionTimestamp: string;
  childrenStyle?: React.CSSProperties;
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
      children
    } = props;

    if (environment.enableProfileCapsFeatures) {
      return (
        <FlexColumn style={{...styles.container, ...props.containerStylesOverride}}>
          <div style={styles.title}>
            { title }
          </div>
          <FlexColumn style={{justifyContent: 'space-between', flex: '1 1 auto'}}>

            { isComplete ? (
              <React.Fragment>
                <div style={props.childrenStyle}>
                  { children }
                </div>
                <Button disabled={true} data-test-id='completed-button'
                        style={{...styles.button, backgroundColor: colors.success,
                          width: 'max-content', cursor: 'default'}}>
                  <ClrIcon shape='check' style={{marginRight: '0.3rem'}}/>{wasBypassed ? 'Bypassed' : completedButtonText}
                </Button>
              </React.Fragment>
            ) : (
              <React.Fragment>
                <div/>
                <Button
                  type='purplePrimary'
                  style={ styles.button }
                  onClick={ completeStep }
                >
                  { incompleteButtonText }
                </Button>
              </React.Fragment>
            ) }
          </FlexColumn>
        </FlexColumn>
      );
    } else { // TODO (RW-3441): delete this block below once enableProfileCapsFeatures is removed
      return (
        <div style={styles.container}>
          <div style={styles.title}>
            { title }
          </div>
          <div style={{ display: 'flex' }}>
            <div style={styles.buttonContainerToDeleteWithEnableProfileCapsFeatures}>
              { isComplete ? (
                <Button type='purplePrimary' style={ {...styles.button, ...styles.successButton} } disabled={true}>
                  <ClrIcon shape='check' style={ {width: 40, marginLeft: -10 } }/>
                  { wasBypassed ? 'Bypassed By Admin' : completedButtonText }
                </Button>
              ) : (
                <Button type='purplePrimary' style={ styles.button } onClick={ completeStep }>
                  { incompleteButtonText }
                </Button>
              ) }
            </div>
            <div style={styles.detailsContainerToDeleteWithEnableProfileCapsFeatures}>
              { children }
            </div>
          </div>
        </div>
      );
    }
  };

export {
  ProfileRegistrationStepStatus,
  Props as ProfileRegistrationStepStatusProps
};
