import * as React from 'react';

import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {
  SemiBoldHeader
} from 'app/components/headers';
import {DatePicker, TextAreaWithLengthValidationMessage} from 'app/components/inputs';
import {Button} from 'app/components/buttons';
import {workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import {useState} from 'react';
import {TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import {validDate} from 'app/utils/date';

const MIN_REASON = 10;
const MAX_REASON = 4000;

interface Props {
  workspace: string,
  onLock: Function,
  onCancel: Function
}

export const AdminLockRequest = (props: Props) => {
  const [requestReason, setRequestReason] = useState('');
  const [requestDate, setRequestDate] = useState(new Date());
  const [apiError, setApiError] = useState(false);

  const invalidReason = !requestReason || requestReason.length < MIN_REASON || requestReason.length > MAX_REASON;
  const lockButtonDisabled = apiError || invalidReason || validDate(requestDate);

  const getToolTipContent = apiError
    ? 'Error occurred while Locking Workspace'
    : <div>Required to lock workspace:
      <ul>
        {invalidReason && <li>Request Reason (minimum length {MIN_REASON}, maximum {MAX_REASON})</li>}
        {validDate(requestDate) && <li>Valid Request Date (in YYYY-MM-DD Format)</li>}
      </ul></div>;

  const onLockWorkspace = () => {
    const {workspace, onLock} = props;
    const adminLockingRequest = {
      requestReason,
      requestDateInMillis: requestDate.valueOf()
    }

    workspaceAdminApi().setAdminLockedState(workspace, adminLockingRequest)
      .then(() => {
        onLock();
      })
      .catch(error => {
        setApiError(true);
      });
  }

   return <Modal>
     <ModalTitle>
       <SemiBoldHeader>Lock workspace</SemiBoldHeader>
     </ModalTitle>
     <ModalBody>
       {apiError && <label style={{color: colors.danger}}>
         Something went wrong while locking the workspace.
       </label>}

       {/* Text area to enter the reason for locking workspace */}
       <div>
         <label style={{fontWeight: 'bold', color: colors.primary}}>
           Enter reason for researchers on why workspace access is locked
         </label>
       </div>
       <div style={{paddingTop: '0.3rem', paddingBottom: '1rem'}}>
         <label style={{color: colors.primary, paddingBottom: '0.3rem'}}>
           <i>Any message in the input box will automatically be sent to researchers when the
             workspace is locked</i>
         </label>
         <TextAreaWithLengthValidationMessage
             textBoxStyleOverrides={{width: '16rem'}}
             id='LOCKED-REASON'
             initialText=''
             maxCharacters={MAX_REASON}
             onChange={(s: string) => {
               setApiError(false);
               setRequestReason(s);
             }}
             tooLongWarningCharacters={MAX_REASON}
             tooShortWarningCharacters={MIN_REASON}
             tooShortWarning={`Locking Request Reason should be at least ${MIN_REASON} characters long`}
          />
       </div>

       {/* Locking workspace request Date*/}
       <div>
         <div style={{fontWeight: 'bold', color: colors.primary, paddingBottom: '0.3rem'}}>
           Add date of RAB request
         </div>
         <DatePicker
             value={requestDate}
             placeholder='YYYY-MM-DD'
             onChange={e => {
               setApiError(false);
               // ensure that e is a Date - the user may have input a string
               const eAsDate = new Date(e);
               setRequestDate(eAsDate);
             }}
             maxDate={new Date()}
         />
       </div>
     </ModalBody>
     <ModalFooter>
       <Button type='secondary' style={{marginRight: '0.5rem'}} onClick={() => props.onCancel()}>
         CANCEL
       </Button>
       <TooltipTrigger content={getToolTipContent} disabled={!lockButtonDisabled}>
         <Button type='primary' onClick={() => onLockWorkspace()} disabled={lockButtonDisabled}>
           LOCK WORKSPACE
         </Button>
       </TooltipTrigger>
     </ModalFooter>
   </Modal>;
}
