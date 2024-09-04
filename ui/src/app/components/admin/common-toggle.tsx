import * as React from 'react';

import { Toggle } from 'app/components/inputs';
import colors from 'app/styles/colors';

interface CommonToggleProps {
  name: string;
  checked: boolean;
  onToggle: (boolean) => void;
  disabled?: boolean;
}
export const CommonToggle = (props: CommonToggleProps) => {
  const { name, checked, disabled, onToggle } = props;
  return (
    <Toggle
      {...{ checked, disabled, name, onToggle }}
      style={{
        color: colors.primary,
        fontWeight: 500,
        paddingBottom: 0,
        flexGrow: 0,
      }}
      height={24}
      width={50}
    />
  );
};
