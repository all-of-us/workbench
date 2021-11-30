import * as React from 'react';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {
  SemiBoldHeader
} from 'app/components/headers';
import {DatePicker, TextArea} from 'app/components/inputs';
import {Button} from 'app/components/buttons';
import {workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import {useState} from 'react';
import {useToggle} from 'app/utils';
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
  const [showError, setShowError] = useToggle();


  const disableLockWorkspace = !requestReason ||
      requestReason === '' || !requestDate || requestDate?.toString() === '';

  const onLockWorkspace = () => {
    const adminLockingRequest = {
      requestReason,
      requestDateInMillis: requestDate.valueOf()
    }

    workspaceAdminApi().setAdminLockedState(props.workspace, adminLockingRequest)
        .then(() =>  {
          props.onLock()
        })
        .catch(error => {
          if(error.status === 400) {
            setShowError(true);
          }
          })
        .finally(() => {
          props.onClose();
        });
  }

   return <Modal>
     <ModalTitle>
       <SemiBoldHeader>Lock workspace</SemiBoldHeader>
     </ModalTitle>
     <ModalBody>
       {showError && <div>
         Something went Wrong while Locking the workspace
       </div>}
       <div>
         <label style={{fontWeight: 'bold', color: colors.primary}}>Enter reason for researcher on
           why workspace access is locked</label>
       </div>
       <div style={{paddingTop: '0.3rem', paddingBottom: '1rem'}}>
         <label style={{color: colors.primary, paddingBottom: '0.3rem'}}><i>Any message in the input box will automatically
           be sent to researcher when the workspace is locked</i></label>

         <TextArea value={requestReason}
                   onChange={(text) => setRequestReason(text)}/>
       </div>
       <div>
         <div style={{fontWeight: 'bold', color: colors.primary, paddingBottom: '0.3rem'}}>Add date
           of RAB request date</div>
         <DatePicker
             value={requestDate}
             placeholder='YYYY-MM-DD'
             onChange={e => setRequestDate(e)}
             maxDate={new Date()}
         />
       </div>
     </ModalBody>
     <ModalFooter>
       <Button type='secondary' style={{marginRight: '0.5rem'}} onClick={() => props.onClose()}>CANCEL</Button>
       <TooltipTrigger content={'Request Reason & Request Date are required to lock workspace'} disabled={!disableLockWorkspace}>
       <Button type='primary' onClick={() => onLockWorkspace()} disabled={disableLockWorkspace}>LOCK WORKSPACE</Button>
       </TooltipTrigger>
     </ModalFooter>
   </Modal>;
}
