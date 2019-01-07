import * as React from 'react';
export const styles = {
  modalMain: {
    // tricky little bit required to get certain CSSProperties
    // to work properly in this setup
    position: 'fixed' as 'fixed',
    background: 'white',
    borderRadius: '8px',
    width: '30%',
    height: 'auto',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%,-50%)',
    zIndex: 1050
  },

  modalBackdrop: {
    position: 'fixed' as 'fixed',
    top: 0,
    bottom: 0,
    right: 0,
    left: 0,
    backgroundColor: '#313131',
    opacity: .85,
    zIndex: 1040
  },

  modalTitle: {
    marginTop: '4%',
    marginLeft: '5%',
    fontSize: '20px',
    color: '#302973'
  },

  modalBody: {
    fontSize: '14px',
    lineHeight: '.8rem',
    marginLeft: '5%',
    marginTop: '3%'
  },

  modalFooter: {
    display: 'flex' as 'flex',
    justifyContent: 'flex-end' as 'flex-end',
    marginBottom: '.4rem',
    marginRight: '.4rem',
    marginTop: '.4rem'
  }
};

export const Modal = ({style = {}, ...props}) => {
  return <div><div style={{...styles.modalBackdrop}} />
    <div {...props} style={{...styles.modalMain, ...style}} /></div>;
};
export const ModalTitle = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.modalTitle, ...style}} />;
export const ModalBody = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.modalBody, ...style}} />;
export const ModalFooter = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.modalFooter, ...style}} />;
