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
      aria-label={`${idPrefix}-disk`}
      id={`${idPrefix}-disk`}
      name={`${idPrefix}-disk`}
      showButtons
      allowEmpty={false}
      disabled={disabled}
      decrementButtonClassName='p-button-secondary'
      incrementButtonClassName='p-button-secondary'
      value={diskSize}
      inputStyle={styles.largeInputNumber}
      onChange={({ value }) => onChange(value)}
    />
  </FlexRow>
);
