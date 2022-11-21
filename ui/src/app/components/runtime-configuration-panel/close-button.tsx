import { CSSProperties } from 'react';
import * as React from 'react';

import { Clickable } from 'app/components/buttons';
import times from 'assets/icons/times-light.svg';

interface CloseButtonProps {
  onClose: Function;
  style?: CSSProperties;
}
export const CloseButton = (props: CloseButtonProps) => {
  const { onClose, style } = props;
  return (
    <Clickable {...{ style }} onClick={onClose}>
      <img src={times} style={{ height: '27px', width: '17px' }} alt='Close' />
    </Clickable>
  );
};
