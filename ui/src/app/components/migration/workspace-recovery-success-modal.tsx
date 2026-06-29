import { Button } from 'app/components/buttons';
import { Modal } from 'app/components/modals';

interface Props {
  onClose: () => void;
  onReturn: () => void;
}

export const WorkspaceRecoverySuccessModal = ({ onClose, onReturn }: Props) => {
  return (
    <Modal
      title=''
      onRequestClose={onClose}
      style={{
        width: '650px',
      }}
    >
      <div
        style={{
          padding: '32px',
        }}
      >
        <div
          style={{
            fontSize: 28,
            fontWeight: 600,
            color: '#2f2b63',
            marginBottom: 18,
          }}
        >
          Workspace Recovery Request Submitted
        </div>

        <div
          style={{
            fontSize: 15,
            lineHeight: '24px',
            marginBottom: 30,
            color: '#555',
          }}
        >
          Thank you for submitting a request to recover your archived workspace.
          Your request will take approximately 5–7 business days. You'll receive
          an email notification once the recovery has been completed.
        </div>

        <Button type='primary' onClick={onReturn}>
          Return to My Workspaces
        </Button>
      </div>
    </Modal>
  );
};
