import * as React from 'react';
import { InputNumber } from 'primereact/inputnumber';

import { FlexRow } from 'app/components/flex';
import { styles } from 'app/components/runtime-configuration-panel/styles';

export const DiskSizeSelector = ({
  onChange,
  disabled,
  diskSize,
  idPrefix,
  style = {},
}) => {
  return (
    <FlexRow style={{ ...styles.labelAndInput, ...style }}>
      <label style={styles.label} htmlFor={`${idPrefix}-disk-size`}>
        Disk (GB)
      </label>
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
};
