import * as React from 'react';
import ReactModal from 'react-modal';
import {ReactComponent} from '../utils/index';

const styles = {
  modal: {
    borderRadius: 8, position: 'relative',
    padding: '1rem', outline: 'none',
    backgroundColor: 'white', boxShadow: '0 1px 2px 2px rgba(0,0,0,.2)'
  },

  overlay: {
    backgroundColor: 'rgba(49, 49, 49, 0.85)', padding: '2rem 1rem',
    display: 'flex', justifyContent: 'center', alignItems: 'flex-start',
    position: 'fixed', left: 0, right: 0, top: 0, bottom: 0, overflowY: 'auto'
  },

  modalTitle: {
    fontSize: '20px',
    color: '#302973',
    marginBottom: '1rem'
  },

  modalBody: {
    fontSize: '14px',
    lineHeight: '.8rem',
  },

  modalFooter: {
    display: 'flex' as 'flex',
    justifyContent: 'flex-end' as 'flex-end',
    marginTop: '1rem'
  }
};

export const Modal = ({width = 450, ...props}) => {
  return <ReactModal
    parentSelector={() => document.getElementById('popup-root')}
    isOpen
    style={{overlay: styles.overlay, content: {...styles.modal, width}}}
    ariaHideApp={false}
    {...props}
  />;
};

export const ModalTitle = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.modalTitle, ...style}} />;
export const ModalBody = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.modalBody, ...style}} />;
export const ModalFooter = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.modalFooter, ...style}} />;
