import {AuditActionCardListView} from 'app/components/admin/audit-card-list-view';
import {Navigate} from 'app/components/app-router';
import {Button} from 'app/components/buttons';
import {TextInput} from 'app/components/inputs';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {actionToString} from 'app/utils/audit-utils';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useParams} from 'react-router-dom';

const {useEffect, useState} = React;

// TODO(jaycarlton): make a reusable ActionAuditComponent that takes in
//   - method to call to retrieve actions (taking in source ID, rowLimit
export const UserAudit = () => {
  const {username = ''} = useParams();
  const [userActions, setUserActions] = useState([]);
  const [nextUsername, setNextUsername] = useState('');
  const [navigateTo, setNavigateTo] = useState(false);
  const [loading, setLoading] = useState(true);
  const [dbId, setDbId] = useState();
  useEffect(() => {
    setNextUsername(username);
  }, []);

  useEffect(() => {
    const getLogEntries = async() => {
      setLoading(true);
      const rowLimit = 500; // rows to fetch from BigQuery audit table. It's not possible to request a specific number of Actions.
      try {
        const {actions, userDatabaseId, query} = await profileApi().getAuditLogEntries(username, rowLimit);
        console.log(query);
        setDbId(userDatabaseId);
        const renderedString = actions.map(action => actionToString(action)).join('\n');
        console.log(renderedString);
        console.log(actions);
        setUserActions(actions);
      } catch (e) {
        setUserActions([]);
      }
      setLoading(false);
    };

    username.length && getLogEntries();
  }, [username]);

  useEffect(() => {
    navigateTo && setNavigateTo(false);
  }, [navigateTo]);

  const getTitle = () => {
    const timesMillis = userActions.map(action => new Date(action.actionTime).getTime());
    const minTime = new Date(Math.min(...timesMillis)).toDateString();
    const maxTime = new Date(Math.max(...timesMillis)).toDateString();
    return `User ${username} ID ${dbId} Audits from ${minTime} to ${maxTime}`;
  };

  return !loading
    ? <React.Fragment>
        {navigateTo && <Navigate to={`/admin/user-audit/${nextUsername}`}/>}
        <TextInput
          style={{
            width: '15rem',
            margin: '1rem'
          }}
          value = {nextUsername}
          onChange = {setNextUsername}
          label = 'Workbench GSuite Username (without @researchallofus.org)'
        />
        <Button style={{height: '1.5rem', margin: '0.25rem 0.5rem'}}
          onClick={() => setNavigateTo(true)}>
          Audit
        </Button>
        <div>{getTitle()}</div>
        <AuditActionCardListView actions={fp.slice(0, 20, userActions)}/>
      </React.Fragment>
    : <div>Loading Audit for user {username}...</div>;
};

// TODO (extra credit)
// - make all cards in the CardListView  collapsible to a summary, e.g. time + a list of action types
// - make the list of AuditPropertyChangeCards collapsible locally (expand/collapse all would  be nice too)
// - add a filter on  ActionType to all the cards in the view. All selected actions must be present
//   in a card for it to render. I.e. we don't want to hide individual EventBundleCards I dont' think.
// - pagination based on the `before` property in the API.
//     * first fetch the 20 (say) most recent actions (limit = 20, after = null,  before = null)
//     * Take timeestamp of the eoldest card reeturned, set `before` to that value and call again
//     - repeat until no results
// - make a generic CardListView
// - use common card style stuff like React.FunctionComponent<WorkspaceCardMenuProps>
