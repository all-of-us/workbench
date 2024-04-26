import * as React from 'react';
import { IconDefinition } from '@fortawesome/free-solid-svg-icons';

import {
  KebabCircleButton,
  MenuItem,
  SnowmanButton,
} from 'app/components/buttons';
import { PopupTrigger, TooltipTrigger } from 'app/components/popups';

export interface ResourceAction {
  icon?: string;
  faIcon?: IconDefinition;
  displayName: string;
  onClick: () => void;
  disabled: boolean;
  hoverText?: string;
}

export const ResourceActionsMenu = (props: {
  actions: ResourceAction[];
  disabled?: boolean;
  useAppListIcon?: boolean;
  menuButtonTitle?: string;
}) => {
  const { actions, disabled, menuButtonTitle, useAppListIcon } = props;

  return (
    <PopupTrigger
      data-test-id='resource-card-menu'
      side='bottom'
      closeOnClick
      content={
        !disabled && (
          <React.Fragment>
            {actions.map((action, i) => {
              const { hoverText, displayName } = action;
              return (
                <TooltipTrigger key={i} content={hoverText}>
                  <MenuItem {...action}>{displayName}</MenuItem>
                </TooltipTrigger>
              );
            })}
          </React.Fragment>
        )
      }
    >
      {useAppListIcon ? (
        <KebabCircleButton {...{ disabled }} title={menuButtonTitle} />
      ) : (
        <SnowmanButton {...{ disabled }} title={menuButtonTitle} />
      )}
    </PopupTrigger>
  );
};
