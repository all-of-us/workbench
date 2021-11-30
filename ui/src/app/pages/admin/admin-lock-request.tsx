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
  onClose: Function
}

export const AdminLockRequest = (props: Props) => {
  const [requestReason, setRequestReason] = useState('');
  const [requestDate, setRequestDate] = useState(new Date());
  const [showError, setShowError] = useState(false);


  const disableLockWorkspace = !requestReason ||
      requestReason === '' || !requestDate || requestDate?.toString() === '' || showError;

  const getToolTipContent = showError ? 'Error occurred while Locking Workspace' :
      'Request Reason & Request Date are required to lock workspace';

  const onLockWorkspace = () => {
    const {workspace , onClose, onLock} = props;
    const adminLockingRequest = {
      requestReason,
      requestDateInMillis: requestDate.valueOf()
    }

    workspaceAdminApi().setAdminLockedState(workspace, adminLockingRequest)
      .then(() => {
        onLock();
        onClose();
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
         Something went Wrong while Locking the workspace.
       </label>}
       <div>
         <label style={{fontWeight: 'bold', color: colors.primary}}>
           Enter reason for researcher on why workspace access is locked
         </label>
       </div>

       {/* Text area to enter the reason for locking workspace */}
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
       <Button type='secondary' style={{marginRight: '0.5rem'}} onClick={() => props.onClose()}>
         CANCEL
       </Button>
       <TooltipTrigger content={getToolTipContent} disabled={!disableLockWorkspace}>
         <Button type='primary' onClick={() => onLockWorkspace()} disabled={disableLockWorkspace}>
           LOCK WORKSPACE
         </Button>
       </TooltipTrigger>
     </ModalFooter>
   </Modal>;
}
