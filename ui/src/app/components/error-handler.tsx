import * as React from 'react';

import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {faTimes} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TextModal} from 'app/components/text-modal';
import {statusApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {isBlank, reactStyles, withGlobalError, withUserProfile} from 'app/utils';
import {globalErrorStore} from 'app/utils/navigation';
import {openZendeskWidget} from 'app/utils/zendesk';
import {ErrorCode, ErrorResponse, Profile} from 'generated/fetch';
import * as fp from 'lodash/fp';

const styles = reactStyles({
  errorCodeContainer: {
    alignItems: 'center',
    flexWrap: 'wrap'
  },
  errorContent: {
    alignItems: 'flex-start'
  },
  errorHandler: {
    position: 'fixed',
    bottom: '1rem',
    background: colors.white,
    padding: '0.6rem',
    border: `1px solid ${colors.black}`,
    width: '12rem'
  },
  iconStyles: {
    cursor: 'pointer',
    height: 17,
    width: 17
  },
  serverStatusList: {
    margin: '0.5rem 0'
  }
});

interface ServerDownStatus {
  apiDown: boolean;
  firecloudDown: boolean;
  notebooksDown: boolean;
}

interface Props {
  globalError: ErrorResponse;
  profileState: {profile: Profile, reload: Function, updateCache: Function};
}

interface State {
  copiedErrorIdToClipboard: boolean;
  serverDownStatus: ServerDownStatus;
  serverStatusAcknowledged: boolean;
}

// We need to maintain this outside of the component, because new errors could come in at any time and we don't want to
// have it reconstruct/reinitialize with new errors.
const shouldCheckStatus = new BehaviorSubject<any>(true);

export const ErrorHandler = fp.flow(withUserProfile(), withGlobalError())(class extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      copiedErrorIdToClipboard: false,
      serverDownStatus: {apiDown: false, firecloudDown: false, notebooksDown: false},
      serverStatusAcknowledged: false
    };
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.state.serverDownStatus !== prevState.serverDownStatus) {
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
    const serverDownStatus = {firecloudDown: false, notebooksDown: false, apiDown: false};
    await statusApi().getStatus().then(statusResponse => {
      if (statusResponse.firecloudStatus === false) {
        serverDownStatus.firecloudDown = true;
      }
      if (statusResponse.notebooksStatus === false) {
        serverDownStatus.notebooksDown = true;
      }
    }).catch(error => {
      serverDownStatus.apiDown = true;
    });
    shouldCheckStatus.next(true);
    this.setState({serverDownStatus});
  }

  closeError() {
    globalErrorStore.next(undefined);
  }

  copyToClipboard() {
    this.setState({copiedErrorIdToClipboard: true});
    setTimeout(() => {this.setState({copiedErrorIdToClipboard: false}); }, 1000);
  }

  openContactWidget() {
    const {profile} = this.props.profileState;
    profile !== undefined ? openZendeskWidget(
      profile.givenName,
      profile.familyName,
      profile.username,
      profile.contactEmail,
    ) : openZendeskWidget('', '', '', '');
  }

  render() {
    const {globalError} = this.props;
    const {serverDownStatus: {apiDown, firecloudDown, notebooksDown}} = this.state;

    return globalError !== undefined && <React.Fragment>
      {globalError.statusCode === 500 && <div style={styles.errorHandler}>
        <FlexRow style={styles.errorContent}>
          <FlexColumn>
          Unexpected Error
          {!isBlank(globalError.errorUniqueId) && <div>
            Please <Button style={{display: 'inline', padding: 0, fontSize: 14}} type='link'
                           onClick={() => this.openContactWidget()}>
              contact support
            </Button> and use this error code: {globalError.errorUniqueId}
          </div>}
          </FlexColumn>
          <FontAwesomeIcon icon={faTimes} style={styles.iconStyles} onClick={() => this.closeError()} />
        </FlexRow></div>}
      {globalError.statusCode === 503 && <div style={styles.errorHandler}>
        Server is currently busy (503)
        <FontAwesomeIcon icon={faTimes} style={styles.iconStyles} onClick={() => this.closeError()} />
      </div>}
      {globalError.errorCode === ErrorCode.USERDISABLED && <TextModal
          title='This account has been disabled'
          body='Please contact a system administrator to inquire about the status of your account.'
          closeFunction={() => {this.closeError(); }}
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
