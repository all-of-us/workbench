import * as React from 'react';

export const Modal = ({...props}) => {
  return <div>
    <div className='modal-backdrop'></div>
    <div className='modal-main' {...props}>{props.children}</div>
  </div>;
};
export const ModalTitle = ({...props}) =>
  <div {...props} className="modal-title">{props.children}</div>;
export const ModalBody = ({...props}) =>
  <div {...props} className="modal-body">{props.children}</div>;
export const FieldInput = ({...props}) =>
  <input {...props} className={"modal-input " + props.className}></input>;
export const ModalFooter = ({...props}) =>
  <div {...props} className="modal-footer">{props.children}</div>;
export const Error = ({...props}) =>
  <div {...props} className="modal-error">{props.children}</div>;

