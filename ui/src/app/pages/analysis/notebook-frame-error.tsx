import * as React from 'react';
import moment from 'moment';

import {StyledExternalLink} from 'app/components/buttons';
import {reactStyles} from 'app/utils';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {ComputeSecuritySuspendedError} from 'app/utils/runtime-utils';
import {SupportMailto} from 'app/components/support';
import {TooltipTrigger} from 'app/components/popups';
import {supportUrls} from 'app/utils/zendesk';
import {ExclamationTriangleIcon, LockIcon} from 'app/components/clr-icons';

const {useState, useEffect} = React;

const styles = reactStyles({
  previewMessageBase: {
    display: 'flex',
    fontSize: '14px',
    fontWeight: 500,
    lineHeight: '24px',
    marginLeft: 'auto',
    marginRight: 'auto',
    marginTop: '56px',
    maxWidth: '550px',
    padding: '8px',
    textAlign: 'left'
  },
  previewError: {
    background: colors.warning,
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    color: colors.white
  },
  previewForbidden: {
    backgroundColor: colorWithWhiteness(colors.warning, .7),
    border: `1px solid ${colors.warning}`,
    borderRadius: '5px',
    color: colors.primary,
    fontWeight: 400,
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
  FORBIDDEN = 'forbidden',
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
    case ErrorMode.FORBIDDEN:
      return <div style={{...styles.previewMessageBase, ...styles.previewForbidden}}>
        <LockIcon style={{color: colors.warning, margin: '0 0.5rem 0 0.25rem', flexShrink: 0}} className='is-solid'
                 size='30'/>
        <div>
          {children}
        </div>
      </div>;
    case ErrorMode.ERROR:
      return <div style={{...styles.previewMessageBase, ...styles.previewError}}>
        <ExclamationTriangleIcon style={{margin: '0 0.5rem 0 0.25rem', flexShrink: 0}} className='is-solid' size='30'/>
        <div>
          {children}
        </div>
      </div>;
    default:
      return null;
  }
};

interface SuspendedMessageProps {
  error: ComputeSecuritySuspendedError
}

export const SecuritySuspendedMessage = ({error}: SuspendedMessageProps) => {
  const until = moment(error.params.suspendedUntil);
  const [duration, setDuration] = useState<string>(until.fromNow());

  useEffect(() => {
    const intervalId = setInterval(() => {
      setDuration(until.fromNow())
    }, 1000);
    return () => clearInterval(intervalId);
  }, [error]);

  const untilFull = moment(error.params.suspendedUntil).format('MMMM Do YYYY, h:mm a');
  return <div data-test-id="security-suspended-msg">
    <div>
    {until.isAfter(new Date()) ?
       <>
         <b>Your analysis environment is suspended due to security egress concerns</b>.&nbsp;
         Your runtime will become available again
         {/* Line break here to avoid splitting duration tooltip trigger. */}
         <br/>
         <TooltipTrigger content={<div>{untilFull}</div>}>
           <b style={{textDecoration: 'underline'}}>{duration}</b>
         </TooltipTrigger>.
       </> : <>
         Your analysis environment was temporarily suspended but is now available for use.
         Reload the page to continue.
       </>}
      &nbsp;Please <b>check your contact email inbox</b> for follow-up and respond
      promptly to verify your activity.
    </div>

    <br/>
    <div>To learn how to avoid common causes of accidental egress, please review
      this <StyledExternalLink href={supportUrls.egressFaq} target='_blank'>
        support article
      </StyledExternalLink> or contact <SupportMailto/> with additional questions.
    </div>
  </div>
};
