import * as React from 'react';
import * as fp from 'lodash/fp';
import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { ErrorCode, ErrorResponse, Profile } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TextModal } from 'app/components/text-modal';
import { statusApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  isBlank,
  reactStyles,
  withSystemError,
  withUserProfile,
} from 'app/utils';
import { systemErrorStore } from 'app/utils/navigation';
import { openZendeskWidget } from 'app/utils/zendesk';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

const styles = reactStyles({
  errorCodeContainer: {
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  errorContent: {
    alignItems: 'flex-start',
  },
  errorHandler: {
    position: 'fixed',
    bottom: '1.5rem',
    background: colors.white,
    padding: '0.9rem',
    border: `1px solid ${colors.black}`,
    width: '18rem',
  },
  iconStyles: {
    cursor: 'pointer',
    height: 17,
    width: 17,
  },
  serverStatusList: {
    margin: '0.75rem 0',
  },
});

interface ServerDownStatus {
  apiDown: boolean;
  firecloudDown: boolean;
  notebooksDown: boolean;
}

interface Props {
  systemError: ErrorResponse;
  profileState: { profile: Profile; reload: Function; updateCache: Function };
}

interface State {
  copiedErrorIdToClipboard: boolean;
  serverDownStatus: ServerDownStatus;
  serverStatusAcknowledged: boolean;
}

// We need to maintain this outside of the component, because new errors could come in at any time and we don't want to
// have it reconstruct/reinitialize with new errors.
const shouldCheckStatus = new BehaviorSubject<any>(true);

export const SystemErrorHandler = fp.flow(
  withUserProfile(),
  withSystemError()
)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        copiedErrorIdToClipboard: false,
        serverDownStatus: {
          apiDown: false,
          firecloudDown: false,
          notebooksDown: false,
        },
        serverStatusAcknowledged: false,
      };
    }

    componentDidUpdate(prevProps, prevState) {
      if (this.state.serverDownStatus !== prevState.serverDownStatus) {
        this.setState({ serverStatusAcknowledged: false });
      }
      const { systemError } = this.props;
      if (systemError !== prevProps.systemError && systemError !== undefined) {
        if (
          (systemError.statusCode === 500 || systemError.statusCode === 503) &&
          shouldCheckStatus.getValue()
        ) {
          // We don't want to spam our server if it is trying to recover and the server check returns a 500 or 503
          shouldCheckStatus.next(false);
          this.pingServer();
        }
      }
    }

    async pingServer() {
      const serverDownStatus = {
        firecloudDown: false,
        notebooksDown: false,
        apiDown: false,
      };
      await statusApi()
        .getStatus()
        .then((statusResponse) => {
          if (statusResponse.firecloudStatus === false) {
            serverDownStatus.firecloudDown = true;
          }
          if (statusResponse.notebooksStatus === false) {
            serverDownStatus.notebooksDown = true;
          }
        })
        .catch(() => {
          serverDownStatus.apiDown = true;
        });
      shouldCheckStatus.next(true);
      this.setState({ serverDownStatus });
    }

    closeError() {
      systemErrorStore.next(undefined);
    }

    copyToClipboard() {
      this.setState({ copiedErrorIdToClipboard: true });
      setTimeout(() => {
        this.setState({ copiedErrorIdToClipboard: false });
      }, 1000);
    }

    openContactWidget() {
      const { profile } = this.props.profileState;
      profile !== undefined
        ? openZendeskWidget(
            profile.givenName,
            profile.familyName,
            profile.username,
            profile.contactEmail
          )
        : openZendeskWidget('', '', '', '');
    }

    render() {
      const { systemError } = this.props;
      const {
        serverDownStatus: { apiDown, firecloudDown, notebooksDown },
      } = this.state;

      return (
        systemError !== undefined && (
          <React.Fragment>
            {systemError.statusCode === 500 && (
              <div style={styles.errorHandler}>
                <FlexRow style={styles.errorContent}>
                  <FlexColumn>
                    Unexpected Error
                    {!isBlank(systemError.errorUniqueId) && (
                      <div>
                        Please{' '}
                        <Button
                          style={{
                            display: 'inline',
                            padding: 0,
                            fontSize: 14,
                          }}
                          type='link'
                          onClick={() => this.openContactWidget()}
                        >
                          contact support
                        </Button>{' '}
                        and use this error code: {systemError.errorUniqueId}
                      </div>
                    )}
                  </FlexColumn>
                  <FontAwesomeIcon
                    icon={faTimes}
                    style={styles.iconStyles}
                    onClick={() => this.closeError()}
                  />
                </FlexRow>
              </div>
            )}
            {systemError.statusCode === 503 && (
              <div style={styles.errorHandler}>
                Server is currently busy (503)
                <FontAwesomeIcon
                  icon={faTimes}
                  style={styles.iconStyles}
                  onClick={() => this.closeError()}
                />
              </div>
            )}
            {systemError.errorCode === ErrorCode.USER_DISABLED && (
              <TextModal
                title='This account has been disabled'
                body='Please contact a system administrator to inquire about the status of your account.'
                closeFunction={() => {
                  this.closeError();
                }}
                buttonText='Close'
              />
            )}
            {(apiDown || firecloudDown || notebooksDown) &&
              !this.state.serverStatusAcknowledged && (
                <Modal>
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
                    <Button
                      onClick={() =>
                        this.setState({ serverStatusAcknowledged: true })
                      }
                    >
                      Close
                    </Button>
                  </ModalFooter>
                </Modal>
              )}
          </React.Fragment>
        )
      );
    }
  }
);
