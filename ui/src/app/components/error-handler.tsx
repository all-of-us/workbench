import * as React from 'react';

import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {ClrIcon} from 'app/components/icons';
import {TextModal} from 'app/components/text-modal';
import {reactStyles, withGlobalError} from 'app/utils';
import {globalErrorStore} from 'app/utils/navigation';
import {ErrorCode, ErrorResponse} from 'generated/fetch';
import colors from 'app/styles/colors';
import {statusApi} from 'app/services/swagger-fetch-clients';
import {Modal, ModalBody, ModalFooter, ModalTitle} from "./modals";
import {Button} from "./buttons";

const styles = reactStyles({
  errorHandler: {
    position: 'fixed',
    bottom: '1rem',
    background: colors.white,
    padding: '0.6rem',
    border: `1px solid ${colors.black}`
  },
  iconStyles: {
    cursor: 'pointer',
  },
  serverStatusList: {
    margin: '0.5rem 0'
  }
});

interface ServerStatus {
  apiDown: boolean;
  firecloudDown: boolean;
  notebooksDown: boolean;
}

interface Props {
  globalError: ErrorResponse;
}

interface State {
  serverStatus: ServerStatus;
  serverStatusAcknowledged: boolean;
}

// We need to maintain this outside of the component, because new errors could come in at any time and we don't want to
// have it reconstruct/reinitialize with new errors.
const shouldCheckStatus = new BehaviorSubject<any>(true);

export const ErrorHandler = withGlobalError()(class extends React.Component<Props, State> {
  obs;
  constructor(props: Props) {
    super(props);
    this.state = {
      serverStatus: {apiDown: false, firecloudDown: false, notebooksDown: false},
      serverStatusAcknowledged: false
    };
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.state.serverStatus !== prevState.serverStatus) {
      this.setState({serverStatusAcknowledged: false});
    }
    const {globalError} = this.props;
    if (globalError !== prevProps.globalError && globalError !== undefined) {
      if ((globalError.statusCode === 500
          || globalError.statusCode === 503)
          && shouldCheckStatus.getValue()) {
        // We don't want to spam our server if it is trying to recover and the server check returns a 500 or 503
        shouldCheckStatus.next(false);
        this.pingServer();
      }
    }
  }

  async pingServer() {
    const serverStatus = {firecloudDown: false, notebooksDown: false, apiDown: false};
    await statusApi().getStatus().then(statusResponse => {
      if (statusResponse.firecloudStatus === false) {
        serverStatus.firecloudDown = true;
      }
      if (statusResponse.notebooksStatus === false) {
        serverStatus.notebooksDown = true;
      }
    }).catch(error => {
      serverStatus.apiDown = true;
    });
    shouldCheckStatus.next(true);
    this.setState({serverStatus});
  }

  closeError() {
    globalErrorStore.next(undefined);
  }

  render() {
    const {globalError} = this.props;
    const {apiDown, firecloudDown, notebooksDown} = this.state.serverStatus;

    return globalError !== undefined && <React.Fragment>
      {globalError.statusCode === 500 && <div style={styles.errorHandler}>
        Server Error (500)
        <ClrIcon shape='times' style={styles.iconStyles} onClick={() => this.closeError()} />
        </div>}
      {globalError.statusCode === 503 && <div style={styles.errorHandler}>
        Server is currently busy (503)
        <ClrIcon shape='times' style={styles.iconStyles} onClick={() => this.closeError()} />
      </div>}
      {globalError.errorCode === ErrorCode.USERDISABLED && <TextModal
          title='This account has been disabled'
          body='Please contact a system administrator to inquire about the status of your account.'
          closeFunction={() => {this.closeError()}}
          buttonText='Close'
      />}
      {(apiDown || firecloudDown || notebooksDown) && !this.state.serverStatusAcknowledged && <Modal>
        <ModalTitle>Service Problems</ModalTitle>
        <ModalBody>
            <div>One or more of our services is currently down:</div>
            <ul style={styles.serverStatusList}>
              {apiDown && <li>Server</li>}
              {firecloudDown && <li>Workspaces Service</li>}
              {notebooksDown && <li>Notebooks Service</li>}
            </ul>
            <div>Please try again later.</div>
        </ModalBody>
        <ModalFooter>
            <Button onClick={() => this.setState({serverStatusAcknowledged: true})}>Close</Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }
});
