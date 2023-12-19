import * as React from 'react';
import { CSSProperties } from 'react';
import { InputNumber } from 'primereact/inputnumber';

import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';

interface Props {
  onChange: (size: number) => void;
  disabled: boolean;
  diskSize: number;
  idPrefix: string;
  style?: CSSProperties;
}
export const DiskSizeSelector = ({
  onChange,
  disabled,
  diskSize,
  idPrefix,
  style = {},
}: Props) => (
  <FlexRow style={{ ...styles.labelAndInput, ...style }}>
    <label style={styles.label}>Disk (GB)</label>
    <InputNumber
      id={`${idPrefix}-disk`}
      showButtons
      disabled={disabled}
      decrementButtonClassName='p-button-secondary'
      incrementButtonClassName='p-button-secondary'
      value={diskSize}
      inputStyle={styles.inputNumber}
      onChange={({ value }) => onChange(value)}
    />
  </FlexRow>
);
