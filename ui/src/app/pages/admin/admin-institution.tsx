import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {SemiBoldHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {navigateByUrl} from 'app/utils/navigation';
import {Institution} from 'generated/fetch';
import {DuaType, OrganizationType} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';
import {OrganizationTypeOptions} from './admin-institution-options';
import {WithSpinnerOverlayProps} from "app/components/with-spinner-overlay";


const styles = reactStyles({
  pageHeader: {
    fontSize: '18px',
    lineHeight: '22px',
    marginBottom: '1rem',
    marginTop: '0.5rem'
  },
  header: {
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '18px',
    color: colors.primary,
    width: '3rem',
    textAlign: 'start',
    height: '4rem',
  },
  text: {
    fontSize: '14px',
    letterSpacing: 0,
    lineHeight: '18px',
    color: colors.primary,
    verticalAlign: 'top',
    textOverflow: 'ellipsis',
    overflow: 'auto',
    height: '4rem',
    width: '5rem',
  }
});

interface State {
  loadingInstitutions: boolean;
  institutions: Array<Institution>;
  institutionLoadError: boolean;
}

export class AdminInstitution extends React.Component<WithSpinnerOverlayProps, State> {
  constructor(props) {
    super(props);
    this.state = {
      loadingInstitutions: true,
      institutions: [],
      institutionLoadError: false
    };
  }

  async componentDidMount() {
    this.props.hideSpinner();
    try {
      const details = await institutionApi().getInstitutions();
      this.setState({
        loadingInstitutions: false,
        institutions: details.institutions
      });
    } catch (e) {
      this.setState({
        loadingInstitutions: false,
        institutionLoadError: true
      });
    }
  }

  renderInstitutionName(row, col) {
    const link = 'admin/institution/edit/' + row['shortName'];
    return <a href={link}> {row['displayName']}</a>;
  }

  renderOrganizationType(row, col) {
    // This should fail if the organization value is not in list
    const organizationLabel = OrganizationTypeOptions
      .filter(organization => organization.value === row['organizationTypeEnum'])[0].label;
    if (row['organizationTypeEnum'] === OrganizationType.OTHER) {
      return organizationLabel + ' - ' + row['organizationTypeOtherText'];
    }
    return organizationLabel;
  }

  renderDuaType(row, col) {
    return row['duaTypeEnum'] === DuaType.RESTRICTED ? 'Individual' : 'Master';
  }

  // If email domain list has more than 4 entries show top 4 and replace others with ...
  renderEmailDomain(row, col) {
    const emailDomain = fp.take(4, row['emailDomains']).join('\n') ;
    if (row['emailDomains'] && row['emailDomains'].length > 4) {
      return emailDomain + '...';
    }
    return emailDomain;
  }

  // If email address list has more than 4 entries show top 4 and replace others with ...
  renderEmailAddress(row, col) {
    const emailAddresses = fp.take(4, row['emailAddresses']).join('\n') ;
    if (row['emailAddresses'] && row['emailAddresses'].length > 4) {
      return emailAddresses + '...';
    }
    return emailAddresses;
  }

  render() {
    const {institutions, institutionLoadError, loadingInstitutions} = this.state;
    return <div>
      <FadeBox style={{marginTop: '1rem', marginLeft: '1rem'}}>
        <SemiBoldHeader style={styles.pageHeader}>
          <label>Institution admin table</label>
              <Button type='secondaryLight'
                      style={{padding: '0rem', marginTop: '0.3rem', verticalAlign: 'sub'}}
                      onClick={() => navigateByUrl('admin/institution/add')}
                      data-test-id='add-institution'>
                <ClrIcon shape='plus-circle' class='is-solid' size={20}/>
              </Button>
        </SemiBoldHeader>
        {institutionLoadError && <div style={{color: colors.danger}}>
          Error while loading Institution. Please try again later</div>}
        <DataTable data-test-id='institution-datatable' value={institutions} paginator={true}
                   rows={10} scrollable={true} frozenWidth='7rem' loading={loadingInstitutions}
                   paginatorTemplate='CurrentPageReport FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink  RowsPerPageDropdown'
                   currentPageReportTemplate='Showing {first} to {last} of {totalRecords} entries'>
          <Column field='displayName' header='Institution Name' body={this.renderInstitutionName}
                  bodyStyle={styles.text} headerStyle={styles.header} frozen={true}/>
          <Column field='organizationTypeEnum' header='Institution Type'
                  body={this.renderOrganizationType} bodyStyle={styles.text}
                  headerStyle={styles.header}/>
          <Column field='duaTypeEnum' header='Agreement Type' body={this.renderDuaType}
                  bodyStyle={styles.text} headerStyle={styles.header}/>
          <Column field='emailDomains' header='Accepted Domain List' body={this.renderEmailDomain}
                  bodyStyle={styles.text} headerStyle={styles.header}/>
          <Column field='emailAddresses' header='Accepted Email List' body={this.renderEmailAddress}
                  bodyStyle={styles.text} headerStyle={styles.header}/>
          <Column field='userInstructions' header='User Email Instruction' bodyStyle={styles.text}
                  headerStyle={{...styles.header, width: '5rem'}}/>
        </DataTable>
      </FadeBox>
    </div>;
  }
}
