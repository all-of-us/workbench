import { ErrorMessage } from 'app/components/messages';
import { reactStyles } from 'app/utils';
import { AuthorityGuardedAction } from 'app/utils/authorities';

const styles = reactStyles({
  container: {
    display: 'flex',
    justifyContent: 'center',
    marginTop: '20px',
  },
});

export const AuthorityMissing = ({
  action,
}: {
  action: AuthorityGuardedAction;
}) => (
  <div style={styles.container}>
    <ErrorMessage>
      <div>
        Your account lacks the administrative authority for action{' '}
        <strong>"{AuthorityGuardedAction[action]}"</strong>.
      </div>
    </ErrorMessage>
  </div>
);
