import * as fp from 'lodash/fp';
import * as React from 'react';

import {Clickable, MenuItem} from 'app/components/buttons';
import {ResourceCardBase} from 'app/components/card';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SnowmanIcon} from 'app/components/icons';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import {formatWorkspaceResourceDisplayDate, reactStyles} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import {
  getDescription,
  getDisplayName,
  getResourceUrl,
  getTypeString,
  isCohort,
  isCohortReview,
  isConceptSet,
  isDataSet,
  isNotebook,
} from 'app/utils/resources';
import {WorkspaceResource} from 'generated/fetch';
import {CSSProperties, PropsWithChildren} from 'react';

const styles = reactStyles({
  card: {
    marginTop: '1rem',
    justifyContent: 'space-between',
    marginRight: '1rem',
    padding: '0.75rem 0.75rem 0rem 0.75rem',
    boxShadow: '0 0 0 0'
  },
  resourceName: {
    fontSize: '18px', fontWeight: 500, lineHeight: '22px', color: colors.accent,
    cursor: 'pointer', wordBreak: 'break-all', textOverflow: 'ellipsis',
    overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical', textDecoration: 'none'
  },
  resourceDescription: {
    textOverflow: 'ellipsis', overflow: 'hidden', display: '-webkit-box',
    WebkitLineClamp: 4, WebkitBoxOrient: 'vertical'
  },
  lastModified: {
    color: colors.primary,
    fontSize: '11px',
    display: 'inline-block',
    lineHeight: '14px',
    fontWeight: 300,
    marginBottom: '0.2rem'
  },
  resourceType: {
    height: '22px',
    width: 'max-content',
    paddingLeft: '10px',
    paddingRight: '10px',
    borderRadius: '2px',
    display: 'flex',
    justifyContent: 'center',
    color: colors.white,
    fontFamily: 'Montserrat, sans-serif',
    fontSize: '12px',
    fontWeight: 500
  },
  cardFooter: {
    display: 'flex',
    flexDirection: 'column'
  }
});

interface Action {
  icon: string;
  displayName: string;
  onClick: () => void;
  disabled: boolean;
  hoverText?: string;
}

const ResourceActionsMenu = (props: {actions: Action[]}) => {
  const {actions} = props;
  return <PopupTrigger
      data-test-id='resource-card-menu'
      side='bottom'
      closeOnClick
      content={
        <React.Fragment>
          {actions.map((action, i) => {
            return (
                <TooltipTrigger key={i} content={action.hoverText}>
                  <MenuItem
                      icon={action.icon}
                      onClick={() => action.onClick()}
                      disabled={action.disabled}>
                    {action.displayName}
                  </MenuItem>
                </TooltipTrigger>);
          })}
        </React.Fragment>
      }
  >
    <Clickable data-test-id='resource-menu'>
      <SnowmanIcon disabled={false}/>
    </Clickable>
  </PopupTrigger>;
};

const StyledResourceType = (props: {resource: WorkspaceResource}) => {
  const {resource} = props;

  function getColor(): string {
    return fp.cond([
      [isCohort, () => colors.resourceCardHighlights.cohort],
      [isCohortReview, () => colors.resourceCardHighlights.cohortReview],
      [isConceptSet, () => colors.resourceCardHighlights.conceptSet],
      [isDataSet, () => colors.resourceCardHighlights.dataSet],
      [isNotebook, () => colors.resourceCardHighlights.notebook],
    ])(resource);
  }
  return <div data-test-id='card-type'
              style={{...styles.resourceType, backgroundColor: getColor()}}
       >{fp.startCase(fp.camelCase(getTypeString(resource)))}</div>;
};


function canWrite(resource: WorkspaceResource): boolean {
  return resource.permission === 'OWNER' || resource.permission === 'WRITER';
}

function canDelete(resource: WorkspaceResource): boolean {
  return resource.permission === 'OWNER';
}

interface NavProps extends PropsWithChildren<any> {
  resource: WorkspaceResource;
  style?: CSSProperties;
}
const ResourceNavigation = (props: NavProps) => {
  const {resource, style = styles.resourceName, children} = props;
  const url = getResourceUrl(resource);

  function canNavigate(): boolean {
    // can always navigate to notebooks
    return isNotebook(resource) || canWrite(resource);
  }

  function onNavigate() {
    if (isNotebook(resource)) {
      AnalyticsTracker.Notebooks.Preview();
    }
  }

  return <Clickable disabled={!canNavigate()}>
    <a style={style}
       data-test-id='resource-navigation'
       href={url}
       onClick={e => {
         onNavigate();
         navigateAndPreventDefaultIfNoKeysPressed(e, url);
       }}>
      {...children}
    </a>
  </Clickable>;
};


interface Props {
  actions: Action[];
  resource: WorkspaceResource;
  menuOnly: boolean;  // use this component strictly for its actions, without rendering the card
}

class ResourceCard extends React.Component<Props, {}> {

  constructor(props: Props) {
    super(props);
  }

  render() {
    const {resource, menuOnly} = this.props;
    return menuOnly ? <ResourceActionsMenu actions={this.props.actions}/> :
        <ResourceCardBase style={styles.card}
                          data-test-id='card'>
          <FlexColumn style={{alignItems: 'flex-start'}}>
            <FlexRow style={{alignItems: 'flex-start'}}>
              <ResourceActionsMenu actions={this.props.actions}/>
              <ResourceNavigation resource={resource}>{getDisplayName(resource)}</ResourceNavigation>
            </FlexRow>
            <div style={styles.resourceDescription}>{getDescription(resource)}</div>
          </FlexColumn>
          <div style={styles.cardFooter}>
            <div style={styles.lastModified} data-test-id='last-modified'>
              Last Modified: {formatWorkspaceResourceDisplayDate(resource.modifiedTime)}</div>
            <StyledResourceType resource={resource}/>
          </div>
        </ResourceCardBase>;
  }
}

export {
  Action,
  ResourceCard,
  StyledResourceType,
  ResourceNavigation,
  canWrite,
  canDelete,
};
