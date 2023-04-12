import * as React from 'react';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Clickable } from 'app/components/buttons';
import { FlexColumn } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

export const buttonStyles = reactStyles({
  button: {
    opacity: 1,
    boxSizing: 'border-box',
    height: 69,
    width: 108,
    border: '1px solid #979797',
    borderRadius: 9,
    alignItems: 'center',
  },
  disabledButton: {
    opacity: 0.46,
    cursor: 'not-allowed',
    boxSizing: 'border-box',
    height: 69,
    width: 108,
    border: '1px solid #979797',
    borderRadius: 9,
    alignItems: 'center',
  },
  buttonText: {
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontWeight: 'bold',
    letterSpacing: 0,
    lineHeight: '15px',
  },
  disabledButtonText: {
    color: colors.secondary,
    fontFamily: 'Montserrat',
    fontWeight: 'bold',
    letterSpacing: 0,
    lineHeight: '15px',
  },
  buttonIcon: {
    height: 23,
    padding: '0.7em',
  },
});

interface Props {
  onClick: Function;
  icon: IconProp;
  buttonText: string;
  disabled?: boolean;
  disabledTooltip?: string;
}
export const AppsPanelButton = (props: Props) => {
  const { disabled, onClick, icon, buttonText, disabledTooltip } = props;

  const showTooltip = disabled && disabledTooltip;

  return (
    <TooltipTrigger content={disabledTooltip} disabled={!showTooltip}>
      <Clickable
        {...{ disabled, onClick }}
        style={{ padding: '0.5em' }}
        data-test-id={`apps-panel-button-${buttonText}`}
        propagateDataTestId
      >
        <FlexColumn
          style={disabled ? buttonStyles.disabledButton : buttonStyles.button}
        >
          <FontAwesomeIcon {...{ icon }} style={buttonStyles.buttonIcon} />
          <div
            style={
              disabled
                ? buttonStyles.disabledButtonText
                : buttonStyles.buttonText
            }
          >
            {buttonText}
          </div>
        </FlexColumn>
      </Clickable>
    </TooltipTrigger>
  );
};
