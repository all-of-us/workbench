import * as React from 'react';

import { Button } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  container: {
    backgroundColor: colors.white,
    color: colors.purple[0],
    flex: '0 0 420px',
    borderRadius: 8,
    marginBottom: '10px',
    padding: 21
  },
  title: {
    color: colors.purple[0],
    width: 420,
    fontSize: 16,
    fontWeight: 600,
    marginBottom: 6
  },
  buttonContainer: {
    width: 'calc(50%)',
    marginTop: 6
  },
  detailsContainer: {
    width: 'calc(50%)',
    marginLeft: 40
  },
  button: {
    height: 38
  },
  successButton: {
    backgroundColor: colors.green[0],
    cursor: 'default'
  }
});

interface Props {
  title: string;
  wasBypassed: boolean;
  isComplete: boolean;
  incompleteButtonText: string;
  completedButtonText: string;
  completeStep: Function;
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

    return (
      <div style={styles.container}>
        <div style={styles.title}>
          { title }
        </div>
        <div style={{ display: 'flex' }}>
          <div style={styles.buttonContainer}>
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
          <div style={styles.detailsContainer}>
            { children }
          </div>
        </div>
      </div>
    );
  };

export {
  ProfileRegistrationStepStatus,
  Props as ProfileRegistrationStepStatusProps
};
