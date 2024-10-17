import * as React from 'react';
import { CSSProperties } from 'react';

import { Toggle } from 'app/components/inputs';
import colors from 'app/styles/colors';

interface CommonToggleProps {
  name: string;
  checked: boolean;
  onToggle: (value: boolean) => void;
  disabled?: boolean;
  style?: CSSProperties;
}
export const CommonToggle = (props: CommonToggleProps) => {
  const { name, checked, disabled, onToggle, style } = props;
  return (
    <Toggle
      {...{ checked, disabled, name, onToggle }}
      style={{
        color: colors.primary,
        fontWeight: 500,
        paddingBottom: 0,
        flexGrow: 0,
        ...style,
      }}
      height={24}
      width={50}
    />
  );
};
