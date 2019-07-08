import * as React from 'react';
import {Clickable} from "../components/buttons";
import {navigateAndPreventDefaultIfNoKeysPressed} from "../utils/navigation";
import * as fp from "lodash";
import {ResourceCardBase} from "../components/card";
import {reactStyles} from "../utils";
import colors from "../styles/colors";
import {ClrIcon} from "../components/icons";
import {PopupTrigger} from "../components/popups";

const styles = reactStyles({
  card: {
    marginTop: '1rem',
    justifyContent: 'space-between',
    marginRight: '1rem',
    padding: '0.75rem 0.75rem 0rem 0.75rem',
    boxShadow: '0 0 0 0'
  },
  cardName: {
    fontSize: '18px', fontWeight: 500, lineHeight: '22px', color: colors.accent,
    cursor: 'pointer', wordBreak: 'break-all', textOverflow: 'ellipsis',
    overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical', textDecoration: 'none'
  },
  cardDescription: {
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
    borderRadius: '4px 4px 0 0',
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

const defaultProps = {
  marginTop: '1rem'
};

export const ResourceCardTemplate: React.FunctionComponent<{
  actionsDisabled: boolean,
  disabled: boolean,
  resourceUrl: string,
  displayName: string,
  description: string,
  displayDate: string,
  footerText: string,
  footerColor: string,
}> = ({
  actionsDisabled,
  disabled,
  resourceUrl,
  displayName,
  description,
  displayDate,
  footerText,
  footerColor}) => {

  return <ResourceCardBase style={{...styles.card, marginTop: defaultProps.marginTop}} // TODO eric: this is a modified value
                           data-test-id='card'>
    <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-start'}}>
      <div style={{display: 'flex', flexDirection: 'row', alignItems: 'flex-start'}}>
        <PopupTrigger
          data-test-id='resource-card-menu'
          side='bottom'
          closeOnClick
          content={null}
        >
          <Clickable disabled={actionsDisabled} data-test-id='resource-menu'>
            <ClrIcon shape='ellipsis-vertical' size={21}
                     style={{color: actionsDisabled ? '#9B9B9B' : '#2691D0', marginLeft: -9,
                       cursor: actionsDisabled ? 'auto' : 'pointer'}}/>
          </Clickable>
        </PopupTrigger>

        <Clickable disabled={disabled}>
          <a style={styles.cardName}
             data-test-id='card-name'
             href={resourceUrl}
             onClick={e => {
               navigateAndPreventDefaultIfNoKeysPressed(e, resourceUrl);
             }}> {displayName}
          </a>
        </Clickable>
      </div>
      <div style={styles.cardDescription}>{description}</div>
    </div>
    <div style={styles.cardFooter}>
      <div style={styles.lastModified} data-test-id='last-modified'>
        Last Modified: {displayDate}</div>
      <div style={{...styles.resourceType, backgroundColor: footerColor}}
           data-test-id='card-type'>
        {fp.startCase(fp.camelCase(footerText))}</div>
    </div>
  </ResourceCardBase>
};
