import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import {StyledExternalLink} from 'app/components/buttons';
import {hasNewValidProps, reactStyles, withCurrentWorkspace} from 'app/utils';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import { SecuritySuspendedErrorParameters } from 'generated/fetch';
import { ComputeSecuritySuspendedError } from 'app/utils/runtime-utils';
import { SupportMailto } from 'app/components/support';

const styles = reactStyles({
  previewMessageBase: {
    marginLeft: 'auto',
    marginRight: 'auto',
    marginTop: '56px'
  },
  previewError: {
    background: colors.warning,
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    color: colors.white,
    display: 'flex',
    fontSize: '14px',
    fontWeight: 500,
    maxWidth: '550px',
    padding: '8px',
    textAlign: 'left'
  },
  previewInvalid: {
    color: colorWithWhiteness(colors.dark, .6),
    fontSize: '18px',
    fontWeight: 600,
    lineHeight: '32px',
    maxWidth: '840px',
    textAlign: 'center'
  },
})

export enum ErrorMode {
  NONE = 'none',
  INVALID = 'invalid',
  ERROR = 'error'
}

interface Props {
  errorMode?: ErrorMode;
  children: React.ReactNode;
}

export const NotebookFrameError = ({errorMode = ErrorMode.ERROR, children}: Props) => {
  switch(errorMode) {
    case ErrorMode.INVALID:
      return <div style={{...styles.previewMessageBase, ...styles.previewInvalid}}>
        {children}
      </div>;
    case ErrorMode.ERROR:
      return <div style={{...styles.previewMessageBase, ...styles.previewError}}>
        <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid'
                 shape='exclamation-triangle' size='30'/>
        {children}
      </div>;
    default:
      return null;
  }
};

interface SuspendedMessageProps {
  error: ComputeSecuritySuspendedError
}

export const SecuritySuspendedMessage = ({error}: SuspendedMessageProps) => {
  const duration = "TODO"
  return <>
    Your runtime has been suspended due to security egress concerns. Your
    runtime will become available again in {duration}. Please promptly check
    your contact email to verify your activity.

    To learn how to avoid common causes of accidental egress, please review
    this
    <StyledExternalLink href={"TODO"} target='_blank'>
      support article
    </StyledExternalLink>, or contact <SupportMailto/> with additional questions.
  </>
};
