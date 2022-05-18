import * as React from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { AdminTableUser, Profile } from 'generated/fetch';

import { AdminUserLink } from 'app/components/admin/admin-user-link';
import { StyledRouterLink } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { Ban, Check } from 'app/components/icons';
import { SpinnerOverlay } from 'app/components/spinners';
import { TierBadge } from 'app/components/tier-badge';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { AdminUserBypass } from 'app/pages/admin/user/admin-user-bypass';
import { userAdminApi } from 'app/services/swagger-fetch-clients';
import {
  cond,
  reactStyles,
  usernameWithoutDomain,
  withUserProfile,
} from 'app/utils';
import {
  AuthorityGuardedAction,
  hasAuthorityForAction,
} from 'app/utils/authorities';
import { getAdminUrl } from 'app/utils/institutions';
import moment from 'moment';

const styles = reactStyles({
  colStyle: {
    fontSize: 12,
    height: '60px',
    lineHeight: '0.5rem',
    overflow: 'hidden',
    padding: '.5em',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  tableStyle: {
    fontSize: 12,
    minWidth: 1200,
  },
  enabledIconStyle: {
    fontSize: 16,
    display: 'flex',
    margin: 'auto',
  },
});

const EnabledIcon = () => (
  <span>
    <Check style={styles.enabledIconStyle} />
  </span>
);
const DisabledIcon = () => (
  <span>
    <Ban style={styles.enabledIconStyle} />
  </span>
);

// Ideally I'd create a functional component here, but DataTable doesn't like that.
// see discussion at https://github.com/all-of-us/workbench/pull/6696#discussion_r876181248

interface ColumnProps {
  header: string;
  field: string;
  sortField?: string;
  filterable: boolean;
  headerWidth: number;
}
const constructColumn = (props: ColumnProps) => {
  const { header, field, sortField, filterable, headerWidth } = props;
  return (
    <Column
      {...{ header, field, sortField }}
      sortable={!!sortField}
      excludeGlobalFilter={!filterable}
      filterField={filterable && sortField}
      filterMatchMode={filterable && 'contains'}
      bodyStyle={styles.colStyle}
      headerStyle={{ ...styles.colStyle, width: headerWidth }}
    />
  );
};
interface DataTableFields {
  name: JSX.Element;
  nameText: string;
  username: JSX.Element;
  usernameText: string;
  contactEmail: JSX.Element;
  contactEmailText: string;
  institutionName: JSX.Element | string;
  institutionNameText: string;
  enabled: JSX.Element;
  dataAccess: JSX.Element;
  bypass: JSX.Element;
  firstSignInTime: string;
}

interface Props extends WithSpinnerOverlayProps {
  profileState: {
    profile: Profile;
    reload: Function;
    updateCache: Function;
  };
}

interface State {
  contentLoaded: boolean;
  filter: string;
  loading: boolean;
  users: AdminTableUser[];
}

/**
 * Users with the ACCESS_MODULE_ADMIN permission use this
 * to manually set (approve/reject) access module bypasses.
 */
export const AdminUserTable = withUserProfile()(
  class extends React.Component<Props, State> {
    debounceUpdateFilter: Function;
    constructor(props) {
      super(props);
      this.state = {
        contentLoaded: false,
        filter: '',
        loading: false,
        users: [],
      };
      this.debounceUpdateFilter = fp.debounce(300, (filterString) =>
        this.setState({ filter: filterString })
      );
    }

    async componentDidMount() {
      this.props.hideSpinner();
      this.setState({ contentLoaded: false });
      await this.loadProfiles();
      this.setState({ contentLoaded: true });
    }

    async loadProfiles() {
      const userListResponse = await userAdminApi().getAllUsers();
      this.setState({ users: userListResponse.users });
    }

    dataAccessContents(user: AdminTableUser): JSX.Element {
      return (
        <FlexRow style={{ justifyContent: 'space-evenly' }}>
          {cond(
            [user.disabled, () => <div>N/A</div>],
            [
              user.accessTierShortNames.length > 0,
              () => (
                <>
                  {user.accessTierShortNames.map((accessTierShortName) => (
                    <TierBadge
                      {...{ accessTierShortName }}
                      key={accessTierShortName}
                    />
                  ))}
                </>
              ),
            ],
            () => (
              <div>No Access</div>
            )
          )}
        </FlexRow>
      );
    }

    displayInstitutionName(tableRow: AdminTableUser) {
      const shouldShowLink =
        tableRow.institutionShortName &&
        hasAuthorityForAction(
          this.props.profileState.profile,
          AuthorityGuardedAction.INSTITUTION_ADMIN
        );
      return shouldShowLink ? (
        <StyledRouterLink
          path={getAdminUrl(tableRow.institutionShortName)}
          target='_blank'
        >
          {tableRow.institutionName}
        </StyledRouterLink>
      ) : (
        tableRow.institutionName
      );
    }

    /**
     * Returns a formatted timestamp, or an empty string if the timestamp is zero or null.
     */
    formattedTimestampOrEmptyString(timestampSecs: number | null): string {
      if (timestampSecs) {
        return moment.unix(timestampSecs / 1000).format('lll');
      } else {
        return '';
      }
    }

    convertProfilesToFields(users: AdminTableUser[]): DataTableFields[] {
      return users
        .map((user) => ({
          // 'text' fields are used for sorting

          name: (
            <AdminUserLink username={user.username} target='_blank'>
              {user.familyName + ', ' + user.givenName}
            </AdminUserLink>
          ),
          nameText: user.familyName + ' ' + user.givenName,

          username: (
            <AdminUserLink username={user.username} target='_blank'>
              {usernameWithoutDomain(user.username)}
            </AdminUserLink>
          ),
          usernameText: usernameWithoutDomain(user.username),

          contactEmail: (
            <a href={`mailto:${user.contactEmail}`}>{user.contactEmail}</a>
          ),
          contactEmailText: user.contactEmail,

          institutionName: this.displayInstitutionName(user),
          institutionNameText: user.institutionName,

          enabled: user.disabled ? <DisabledIcon /> : <EnabledIcon />,
          dataAccess: this.dataAccessContents(user),
          bypass: (
            <AdminUserBypass
              user={{ ...user }}
              onBypassModuleUpdate={() => this.loadProfiles()}
            />
          ),

          firstSignInTime: this.formattedTimestampOrEmptyString(
            user.firstSignInTime
          ),
        }))
        .sort((f1: DataTableFields, f2: DataTableFields) =>
          f1.nameText.localeCompare(f2.nameText)
        );
    }

    render() {
      const { contentLoaded, filter, loading, users } = this.state;
      return (
        <div style={{ position: 'relative', padding: '1em' }}>
          <h2>User Admin Table</h2>
          {loading && (
            <SpinnerOverlay
              opacity={0.6}
              overrideStylesOverlay={{
                alignItems: 'flex-start',
                marginTop: '2rem',
              }}
            />
          )}
          {!contentLoaded && <div>Loading user profiles...</div>}
          {contentLoaded && (
            <div>
              <input
                data-test-id='search'
                style={{ marginBottom: '.5em', width: '300px' }}
                type='text'
                placeholder='Search'
                onChange={(e) => this.debounceUpdateFilter(e.target.value)}
              />
              <DataTable
                value={this.convertProfilesToFields(users)}
                frozenWidth='200px'
                globalFilter={filter}
                paginator={true}
                rows={10}
                rowsPerPageOptions={[5, 10, 50, 100]}
                scrollable
                style={styles.tableStyle}
              >
                <Column
                  header='Name'
                  field='name'
                  sortField='nameText'
                  filterField='nameText'
                  filterMatchMode='contains'
                  sortable={true}
                  frozen={true}
                  bodyStyle={styles.colStyle}
                  headerStyle={{ ...styles.colStyle, width: '200px' }}
                />
                {constructColumn({
                  header: 'Username',
                  field: 'username',
                  sortField: 'usernameText',
                  filterable: true,
                  headerWidth: 200,
                })}
                <Column
                  header='Contact Email'
                  field='contactEmail'
                  sortField='contactEmailText'
                  filterField='contactEmailText'
                  filterMatchMode='contains'
                  sortable={true}
                  bodyStyle={styles.colStyle}
                  headerStyle={{ ...styles.colStyle, width: '180px' }}
                />
                <Column
                  header='Institution'
                  field='institutionName'
                  sortField='institutionNameText'
                  filterField='institutionNameText'
                  filterMatchMode='contains'
                  sortable={true}
                  bodyStyle={styles.colStyle}
                  headerStyle={{ ...styles.colStyle, width: '180px' }}
                />
                <Column
                  header='Enabled'
                  field='enabled'
                  sortable={false}
                  excludeGlobalFilter={true}
                  bodyStyle={{ ...styles.colStyle }}
                  headerStyle={{ ...styles.colStyle, width: '150px' }}
                />
                <Column
                  header='Data Access'
                  field='dataAccess'
                  sortable={false}
                  excludeGlobalFilter={true}
                  bodyStyle={{ ...styles.colStyle }}
                  headerStyle={{ ...styles.colStyle, width: '100px' }}
                />
                <Column
                  header='Access Module Bypass'
                  field='bypass'
                  sortable={false}
                  excludeGlobalFilter={true}
                  bodyStyle={{ ...styles.colStyle }}
                  headerStyle={{ ...styles.colStyle, width: '150px' }}
                />
                <Column
                  header='First Sign-in'
                  field='firstSignInTime'
                  sortField='firstSignInTime'
                  sortable={true}
                  excludeGlobalFilter={true}
                  bodyStyle={{ ...styles.colStyle }}
                  headerStyle={{ ...styles.colStyle, width: '180px' }}
                />
              </DataTable>
            </div>
          )}
        </div>
      );
    }
  }
);
