import * as React from 'react';

import { Toggle } from 'app/components/inputs';

interface CommonToggleProps {
  name: string;
  checked: boolean;
  dataTestId: string;
  onToggle: (boolean) => void;
  disabled?: boolean;
}
export const CommonToggle = (props: CommonToggleProps) => {
  const { name, checked, dataTestId, disabled, onToggle } = props;
  return (
    <Toggle
      {...{ checked, dataTestId, disabled, name, onToggle }}
      style={{ paddingBottom: 0, flexGrow: 0 }}
      height={24}
      width={50}
    />
  );
};
