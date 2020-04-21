import * as React from 'react';

import { Button } from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {environment} from 'environments/environment';

const styles = reactStyles({
  container: {
    backgroundColor: colors.white,
    color: colors.primary,
    flex: '0 0 420px',
    borderRadius: 8,
    marginBottom: '10px',
    padding: 21
  },
  title: {
    color: colors.primary,
    width: 420,
    fontSize: 16,
    fontWeight: 600,
    marginBottom: 6
  },
  button: {
    height: 38
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
  title: any;
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
      completionTimestamp,
      children
    } = props;

    if (environment.enableProfileCapsFeatures) {
      return (
        <div style={styles.container}>
          <div style={styles.title}>
            { title }
          </div>
          <FlexColumn>
            <div>
              { isComplete ? (
                <FlexRow>
                  <FlexColumn>
                    <div>{ wasBypassed ? 'Bypassed By Admin on:' : completedButtonText + ' on:' }
                    </div>
                    <div>{new Date(completionTimestamp).toDateString()}</div>
                    <Button disabled={true} data-test-id='completed-button'
                            style={{backgroundColor: colors.success,
                              width: 'max-content', marginTop: '1rem',
                              cursor: 'default'}}>
                      <ClrIcon shape='check' style={{marginRight: '0.3rem'}}/>{completedButtonText}
                    </Button>
                  </FlexColumn>

                </FlexRow>
              ) : (
                <Button
                  type='purplePrimary'
                  style={ styles.button }
                  onClick={ completeStep }
                >
                  { incompleteButtonText }
                </Button>
              ) }
            </div>
            <div style={{marginLeft: '2.5rem', ...props.childrenStyle}}>
              { children }
            </div>
          </FlexColumn>
        </div>
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
                <Button
                  type='purplePrimary'
                  style={ {...styles.button, ...styles.successButton} }
                  disabled={true}
                >
                  <ClrIcon shape='check' style={ {width: 40, marginLeft: -10 } }/>
                  { wasBypassed ?
                    'Bypassed By Admin' : completedButtonText }
                </Button>
              ) : (
                <Button
                  type='purplePrimary'
                  style={ styles.button }
                  onClick={ completeStep }
                >
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
