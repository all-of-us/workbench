import * as React from 'react';

import { Button } from 'app/components/buttons';

interface Props {
  label: string;
  buttonText?: string;
  onClick: () => void;
  disabled: boolean;
}
export const CommonButton = ({
  label,
  buttonText = label,
  onClick,
  disabled,
}: Props) => (
  <Button {...{ onClick, disabled }} aria-label={label}>
    {buttonText}
  </Button>
);
