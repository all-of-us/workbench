import * as React from 'react';

import { Button } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  box: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 21
  },
  header: {
    color: colors.purple[0],
    width: 420,
    fontSize: 16,
    fontWeight: 600,
    marginBottom: 6
  },
});


interface Props {
  title: string;
  bypassedByAdmin: boolean;
  completed: boolean;
  incompletedButtonText: string;
  completedButtonText: string;
  completeStep: Function;
}

class ProfileRegistrationStepStatus extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
  }

  render() {
    return (
      <div style={{color: '#262262', flex: '0 0 420px', marginBottom: '10px'}}>
        <div style={styles.box}>
          <div style={{...styles.header}}>
            {this.props.title}
          </div>
          <div style={{ display: 'flex' }}>
            <div style={{ width: 'calc(50%)', marginTop: 6 }}>
              { this.props.completed ? (
                <Button
                    type='purplePrimary'
                    style={{ height: 38, backgroundColor: '#8BC990', cursor: 'default'}}
                    disabled={true}
                >
                  <ClrIcon shape='check' style={{width: 40, marginLeft: -10 }}/>
                  { this.props.bypassedByAdmin ?
                    'Bypassed By Admin' : this.props.completedButtonText }
                </Button>
              ) : (
                <Button
                  type='purplePrimary'
                  style={{ height: 38 }}
                  onClick={ this.props.completeStep }
                >
                  { this.props.incompletedButtonText }
                </Button>
              ) }
            </div>
            <div style={{ width: 'calc(50%)', marginLeft: 40 }}>
              { this.props.children }
            </div>
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
