import * as React from 'react';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {
  SemiBoldHeader
} from 'app/components/headers';
import {DatePicker, TextArea} from 'app/components/inputs';
import {Button} from 'app/components/buttons';
import {workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import {useState} from 'react';
import {TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';

interface Props {
  workspace: string,
  onLock: Function,
  onCancel: Function
}

export const AdminLockRequest = (props: Props) => {
  const [requestReason, setRequestReason] = useState('');
  const [requestDate, setRequestDate] = useState(new Date());
  const [showError, setShowError] = useState(false);

  const enableLockButton = requestReason?.length > 0 && requestDate?.toString() !== ''
      && !isNaN(requestDate.valueOf()) && !showError;


  const getToolTipContent = showError ? 'Error occurred while Locking Workspace' :
      'Request Reason & Valid Request Date (in YYYY-MM-DD Format) are required to lock workspace';

  const onLockWorkspace = () => {
    const {workspace , onCancel, onLock} = props;
    const adminLockingRequest = {
      requestReason,
      requestDateInMillis: requestDate.valueOf()
    }

    workspaceAdminApi().setAdminLockedState(workspace, adminLockingRequest)
      .then(() => {
        onLock();
      })
      .catch(error => {
         setShowError(true);
      });
  }

   return <Modal>
     <ModalTitle>
       <SemiBoldHeader>Lock workspace</SemiBoldHeader>
     </ModalTitle>
     <ModalBody>
       {showError && <label style={{color: colors.danger}}>
         Something went wrong while locking the workspace.
       </label>}

       {/* Text area to enter the reason for locking workspace */}
       <div>
         <label style={{fontWeight: 'bold', color: colors.primary}}>
           Enter reason for researcher on why workspace access is locked
         </label>
       </div>
       <div style={{paddingTop: '0.3rem', paddingBottom: '1rem'}}>
         <label style={{color: colors.primary, paddingBottom: '0.3rem'}}>
           <i>Any message in the input box will automatically be sent to researcher when the
             workspace is locked</i>
         </label>
         <TextArea value={requestReason} onChange={(text) => setRequestReason(text)}/>
       </div>

       {/* Locking workspace request Date*/}
       <div>
         <div style={{fontWeight: 'bold', color: colors.primary, paddingBottom: '0.3rem'}}>
           Add date of RAB request date
         </div>
         <DatePicker
             value={requestDate}
             placeholder='YYYY-MM-DD'
             onChange={e => setRequestDate(e)}
             maxDate={new Date()}
         />
       </div>
     </ModalBody>
     <ModalFooter>
       <Button type='secondary' style={{marginRight: '0.5rem'}} onClick={() => props.onCancel()}>
         CANCEL
       </Button>
       <TooltipTrigger content={getToolTipContent} disabled={enableLockButton}>
         <Button type='primary' onClick={() => onLockWorkspace()} disabled={!enableLockButton}>
           LOCK WORKSPACE
         </Button>
       </TooltipTrigger>
     </ModalFooter>
   </Modal>;
}
