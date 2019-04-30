import * as React from 'react';

import { Button } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  container: {
    backgroundColor: '#fff',
    color: '#262262',
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
  body: {
    display: 'flex'
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
    backgroundColor: '#8BC990',
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

class ProfileRegistrationStepStatus extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
  }

  render() {
    return (
      <div style={styles.container}>
        <div style={{...styles.title}}>
          { this.props.title }
        </div>
        <div style={styles.body}>
          <div style={styles.buttonContainer}>
            { this.props.isComplete ? (
              <Button
                  type='purplePrimary'
                  style={{ ...styles.button, ...styles.successButton}}
                  disabled={true}
              >
                <ClrIcon shape='check' style={{width: 40, marginLeft: -10 }}/>
                { this.props.wasBypassed ?
                  'Bypassed By Admin' : this.props.completedButtonText }
              </Button>
            ) : (
              <Button
                type='purplePrimary'
                style={styles.button}
                onClick={ this.props.completeStep }
              >
                { this.props.incompleteButtonText }
              </Button>
            ) }
          </div>
          <div style={styles.detailsContainer}>
            { this.props.children }
          </div>
        </div>
      </div>
    );
  }
}

export {
  ProfileRegistrationStepStatus,
  Props as ProfileRegistrationStepStatusProps
};
