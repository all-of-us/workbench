import * as Color from 'color';
import * as React from 'react';
import * as ReactModal from 'react-modal';

import {Button} from 'app/components/buttons';
import {SpinnerOverlay} from 'app/components/spinners';
import colors from 'app/styles/colors';
import {reactStyles, withStyle} from 'app/utils/index';
import {notificationStore, NotificationStore, profileStore, useStore} from 'app/utils/stores';
import {openZendeskWidget} from 'app/utils/zendesk';
import * as fp from 'lodash/fp';
import {animated, useSpring} from 'react-spring';
import {TextColumn} from './text-column';

const {useEffect} = React;

const styles = reactStyles({
  modal: {
    borderRadius: 8, position: 'relative',
    padding: '1rem', margin: 'auto', outline: 'none',
    backgroundColor: 'white', boxShadow: '0 1px 2px 2px rgba(0,0,0,.2)'
  },

  overlay: {
    backgroundColor: Color(colors.dark).alpha(0.85).toString(), padding: '1rem', display: 'flex',
    position: 'fixed', left: 0, right: 0, top: 0, bottom: 0, overflowY: 'auto',
    // Keep z-index in sync with popups.tsx.
    zIndex: 105
  },

  modalTitle: {
    fontSize: '20px',
    color: colors.primary,
    fontWeight: 600,
    marginBottom: '1rem'
  },

  modalBody: {
    fontSize: '14px',
    lineHeight: '.8rem',
    marginTop: '3%',
    fontWeight: 400

  },

  modalFooter: {
    display: 'flex' as 'flex',
    justifyContent: 'flex-end' as 'flex-end',
    marginTop: '1rem'
  }
});

export const Modal = ({width = 450, loading = false, ...props}) => {
  return <ReactModal
    parentSelector={() => document.getElementById('popup-root')}
    isOpen
    style={{overlay: styles.overlay, content: props.contentStyleOverride || {...styles.modal, width}}}
    ariaHideApp={false}
    {...props}
  >
    {props.children}
    {loading && <SpinnerOverlay/>}
  </ReactModal>;
};


export const AnimatedModal = ({width = 450, ...props}) => {
  const style = {...styles.modal, width};
  const styleSpring = useSpring({...style, from: {width: 450}});

  return <Modal contentStyleOverride={{margin: 'auto'}} {...props}>
    <div>
      <animated.div style={styleSpring}>
        {props.children}
      </animated.div>
    </div>
  </Modal>;
};

export const ModalTitle = withStyle(styles.modalTitle)('div');
export const ModalBody = withStyle(styles.modalBody)('div');
export const ModalFooter = withStyle(styles.modalFooter)('div');

// This modal is rendered when there is data present in the notificationStore - rendered at the router level until Angular is gone
export const NotificationModal = () => {
  const notification = useStore(notificationStore);
  const profile = profileStore.get().profile;
  const {title = '', message = '', showBugReportLink: showBugReportLink = false, onDismiss = fp.noop} = notification || {};

  useEffect(() => onDismiss);

  return notification && <Modal>
    <ModalTitle>{title}</ModalTitle>
    <ModalBody>
        <TextColumn>
          <div>{message}</div>
          {showBugReportLink &&
            <div style={{marginTop: '0.5rem'}}>
                Please <a onClick={() => {
                  openZendeskWidget(profile.givenName, profile.familyName, profile.username, profile.contactEmail);
                  notificationStore.set(null);
                }}> submit a bug report. </a>
            </div>}
        </TextColumn>
    </ModalBody>
    <ModalFooter>
      <Button onClick={() => notificationStore.set(null)}>OK</Button>
    </ModalFooter>
  </Modal>;
};

export const withErrorModal = fp.curry((notificationState: NotificationStore, wrappedFn) => async(...args) => {
  try {
    return await wrappedFn(...args);
  } catch (e) {
    notificationStore.set(notificationState);
  }
});

export const withSuccessModal = fp.curry((notificationState: NotificationStore, wrappedFn) => async(...args) => {
  const response = await wrappedFn(...args);
  notificationStore.set(notificationState);
  return response;
});
