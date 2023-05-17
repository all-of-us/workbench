import * as React from 'react';

import { LinkButton } from 'app/components/buttons';
import { styles } from 'app/components/runtime-configuration-panel/styles';

interface DeletePersistentDiskProps {
  onClick: () => void;
  style?: React.CSSProperties;
}

export const DeletePersistentDiskButton = ({
  onClick,
  style = {},
}: DeletePersistentDiskProps) => (
  <LinkButton
    style={{
      ...styles.deleteLink,
      ...style,
    }}
    aria-label='Delete Persistent Disk'
    onClick={onClick}
  >
    Delete Persistent Disk
  </LinkButton>
);
