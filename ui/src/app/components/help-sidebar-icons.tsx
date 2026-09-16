import * as React from 'react';
import { CSSProperties } from 'react';
import {
  faBook,
  faInfoCircle,
  IconDefinition,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { DEFAULT, switchCase } from '@terra-ui-packages/core-utils';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { supportUrls } from 'app/utils/zendesk';

import { TooltipTrigger } from './popups';

const styles = reactStyles({
  asyncOperationStatusIcon: {
    width: '.75rem',
    height: '.75rem',
    zIndex: 2,
  },
  statusIconContainer: {
    alignSelf: 'flex-end',
    margin: '0 .15rem .15rem auto',
  },
  rotate: {
    animation: 'rotation 2s infinite linear',
  },
  icon: {
    background: colorWithWhiteness(colors.primary, 0.48),
    color: colors.white,
    display: 'table-cell',
    height: '46px',
    width: '45px',
    borderBottom: `1px solid ${colorWithWhiteness(colors.primary, 0.4)}`,
    cursor: 'pointer',
    textAlign: 'center',
    verticalAlign: 'middle',
  },
  compoundStyle: { width: '36px', position: 'absolute' },
  compoundContainerStyle: {
    height: '100%',
    alignItems: 'center',
    justifyContent: 'space-around',
  },
});

const iconStyles = reactStyles({
  active: {
    ...styles.icon,
    background: colorWithWhiteness(colors.primary, 0.55),
  },
  disabled: {
    ...styles.icon,
    cursor: 'not-allowed',
  },
});

export type SidebarIconId = 'help' | 'dataDictionary';

export interface IconConfig {
  id: SidebarIconId;
  disabled: boolean;
  faIcon: IconDefinition;
  label: string;
  showIcon: () => boolean;
  style: CSSProperties;
  tooltip: string;
  hasContent: boolean;
}

const displayFontAwesomeIcon = (icon: IconConfig) => (
  <FontAwesomeIcon
    data-test-id={'help-sidebar-icon-' + icon.id}
    icon={icon.faIcon}
    style={icon.style}
  />
);

interface DisplayIconProps {
  icon: IconConfig;
}
const DisplayIcon = (props: DisplayIconProps) => {
  const { icon } = props;

  return switchCase<SidebarIconId, React.ReactElement>(
    icon.id,
    [
      'dataDictionary',
      () => (
        <a href={supportUrls.dataDictionary} target='_blank'>
          <FontAwesomeIcon
            data-test-id={'help-sidebar-icon-' + icon.id}
            icon={icon.faIcon}
            style={icon.style}
          />
        </a>
      ),
    ],
    [
      DEFAULT,
      () =>
        icon.faIcon === null ? (
          <img
            alt={icon.label}
            data-test-id={'help-sidebar-icon-' + icon.id}
            style={icon.style}
          />
        ) : (
          displayFontAwesomeIcon(icon)
        ),
    ]
  );
};

interface IconConfigProps {
  iconId: SidebarIconId;
}
const iconConfig = (props: IconConfigProps): IconConfig => {
  const { iconId } = props;

  const config: Record<SidebarIconId, IconConfig> = {
    help: {
      id: 'help',
      disabled: false,
      faIcon: faInfoCircle,
      label: 'Help Icon',
      showIcon: () => true,
      style: { fontSize: '21px' },
      tooltip: 'Help Tips',
      hasContent: true,
    },
    dataDictionary: {
      id: 'dataDictionary',
      disabled: false,
      faIcon: faBook,
      label: 'Data Dictionary Icon',
      showIcon: () => true,
      style: { color: colors.white, fontSize: '20px', marginTop: '5px' },
      tooltip: 'Data Dictionary',
      hasContent: false,
    },
  };

  return config[iconId];
};

interface HelpSidebarIconsProps {
  activeIcon: string;
  onIconClick: (icon: IconConfig) => void;
}
export const HelpSidebarIcons = (props: HelpSidebarIconsProps) => {
  const { activeIcon, onIconClick } = props;
  const defaultIcons: SidebarIconId[] = ['help', 'dataDictionary'];
  const keys: SidebarIconId[] = defaultIcons.filter((iconId) =>
    iconConfig({
      iconId,
    }).showIcon()
  );

  const icons = keys.map((iconId) => iconConfig({ iconId }));

  return (
    <>
      {icons.map((icon, i) => (
        <div key={i} style={{ display: 'table' }}>
          <TooltipTrigger content={<div>{icon.tooltip}</div>} side='left'>
            <div
              style={
                activeIcon === icon.id
                  ? iconStyles.active
                  : icon.disabled
                  ? iconStyles.disabled
                  : styles.icon
              }
              onClick={() => {
                if (icon.hasContent && !icon.disabled) {
                  onIconClick(icon);
                }
              }}
            >
              <DisplayIcon {...{ ...props, icon }} />
            </div>
          </TooltipTrigger>
        </div>
      ))}
    </>
  );
};
