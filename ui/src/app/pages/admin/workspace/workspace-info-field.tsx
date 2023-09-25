import * as React from 'react';

import { FlexRow } from 'app/components/flex';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  infoRow: {
    width: '80%',
    maxWidth: '1000px',
  },
  infoLabel: {
    width: '300px',
    minWidth: '180px',
    textAlign: 'right',
    marginRight: '1.5rem',
  },
  infoValue: {
    flex: 1,
    wordWrap: 'break-word',
  },
});

interface LabelProps {
  style?: React.CSSProperties;
  children: any;
}
export const PurpleLabel = ({ style, children }: LabelProps) => {
  return <label style={{ color: colors.primary, ...style }}>{children}</label>;
};

interface InfoProps {
  labelText: string;
  children: any;
}
export const WorkspaceInfoField = ({ labelText, children }: InfoProps) => {
  return (
    <FlexRow style={styles.infoRow}>
      <PurpleLabel style={styles.infoLabel}>{labelText}</PurpleLabel>
      <div style={styles.infoValue}>{children}</div>
    </FlexRow>
  );
};
