import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {SemiBoldHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {ReactWrapperBase} from 'app/utils';
import {reactStyles} from 'app/utils';
import {Institution} from 'generated/fetch';
import {DuaType, OrganizationType} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';


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

export class AdminInstitution extends React.Component<{}, State> {
  constructor(props) {
    super(props);
    this.state = {
      loadingInstitutions: true,
      institutions: [],
      institutionLoadError: false
    };
  }

  async componentDidMount() {
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

  renderOrganizationType(row, col) {
    switch ( row['organizationTypeEnum']) {
      case OrganizationType.ACADEMICRESEARCHINSTITUTION: return 'Academic Research Institution';
      case OrganizationType.HEALTHCENTERNONPROFIT: return 'Health Center Non-Profit';
      case OrganizationType.EDUCATIONALINSTITUTION: return 'Educational Institution';
      case OrganizationType.INDUSTRY: return 'Industry';
      default: return 'Other';
    }
  }

  renderDuaType(row, col) {
    return row['duaTypeEnum'] === DuaType.RESTRICTED ? 'Individual' : 'Master';
  }

  // If email domain is more than 4 show top 4 and replace others with ...
  renderEmailDomain(row, col) {
    const emailDomain = fp.take(4, row['emailDomains']).join('\n') ;
    if (row['emailDomains'] && row['emailDomains'].length > 4) {
      return emailDomain + '...';
    }
    return emailDomain;
  }

  // If email address is more than 4 show top 4 and replace others with ...
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
                      data-test-id='add-institution'>
                <ClrIcon shape='plus-circle' class='is-solid' size={20}/>
              </Button>
        </SemiBoldHeader>
        {institutionLoadError && <div style={{color: colors.danger}}>
          Error while loading Institution please try again later</div>}
        <DataTable data-test-id='institution-datatable' value={institutions} paginator={true}
                   rows={5} scrollable={true} frozenWidth='7rem' loading={loadingInstitutions}>
          <Column field='displayName' header='Institution Name'
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
          <Column field='userInstructions' header='User Instruction' bodyStyle={styles.text}
                  headerStyle={{...styles.header, width: '5rem'}}/>
        </DataTable>
      </FadeBox>
    </div>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class AdminInstitutionComponent extends ReactWrapperBase {
  constructor() {
    super(AdminInstitution, []);
  }
}
