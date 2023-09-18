import * as React from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  Institution,
  InstitutionMembershipRequirement,
  OrganizationType,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { SemiBoldHeader } from 'app/components/headers';
import { ClrIcon } from 'app/components/icons';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { OrganizationTypeOptions } from 'app/pages/admin/admin-institution-options';
import { institutionApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { capStringWithEllipsis, reactStyles } from 'app/utils';
import { orderedAccessTierShortNames } from 'app/utils/access-tiers';
import { getAdminUrl, getTierConfig } from 'app/utils/institutions';
import { NavigationProps } from 'app/utils/navigation';
import { canonicalizeUrl } from 'app/utils/urls';
import { withNavigation } from 'app/utils/with-navigation-hoc';

const styles = reactStyles({
  pageHeader: {
    fontSize: '18px',
    lineHeight: '22px',
    marginBottom: '1.5rem',
    marginTop: '0.75rem',
  },
  header: {
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '18px',
    color: colors.primary,
    width: '4.5rem',
    textAlign: 'start',
    height: '6rem',
  },
  text: {
    fontSize: '14px',
    letterSpacing: 0,
    lineHeight: '18px',
    color: colors.primary,
    verticalAlign: 'top',
    textOverflow: 'ellipsis',
    overflow: 'auto',
    height: '6rem',
    width: '7.5rem',
  },
});

interface Props extends WithSpinnerOverlayProps, NavigationProps {}

interface State {
  loadingInstitutions: boolean;
  institutions: Array<Institution>;
  institutionLoadError: boolean;
}

export const AdminInstitution = fp.flow(withNavigation)(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        loadingInstitutions: true,
        institutions: [],
        institutionLoadError: false,
      };
    }

    async componentDidMount() {
      this.props.hideSpinner();
      try {
        const details = await institutionApi().getInstitutions();
        this.setState({
          loadingInstitutions: false,
          institutions: details.institutions,
        });
      } catch (e) {
        this.setState({
          loadingInstitutions: false,
          institutionLoadError: true,
        });
      }
    }

    renderInstitutionName(institution: Institution) {
      const link = getAdminUrl(institution.shortName);
      return <a href={link}>{institution.displayName}</a>;
    }

    renderOrganizationType(institution: Institution) {
      // This should fail if the organization value is not in list
      const organizationLabel = OrganizationTypeOptions.filter(
        (organization) =>
          organization.value === institution.organizationTypeEnum
      )[0].label;
      if (institution.organizationTypeEnum === OrganizationType.OTHER) {
        return `${organizationLabel} - ${institution.organizationTypeOtherText}`;
      }
      return organizationLabel;
    }

    renderAccessTiers(institution: Institution) {
      return fp.flow(
        fp.filter<string>(
          (tier) =>
            getTierConfig(institution, tier)?.membershipRequirement !==
            InstitutionMembershipRequirement.NO_ACCESS
        ),
        fp.map(fp.capitalize),
        fp.join(', ')
      )(orderedAccessTierShortNames);
    }

    renderInstitutionInstructions(institution: Institution) {
      return capStringWithEllipsis(institution.userInstructions, 300);
    }

    renderRequestAccessUrl(institution: Institution) {
      return institution.requestAccessUrl ? (
        <a href={canonicalizeUrl(institution.requestAccessUrl)} target='_blank'>
          {capStringWithEllipsis(institution.requestAccessUrl, 300)}
        </a>
      ) : (
        ''
      );
    }

    render() {
      const { institutions, institutionLoadError, loadingInstitutions } =
        this.state;
      return (
        <div>
          <FadeBox style={{ marginTop: '1.5rem', marginLeft: '1.5rem' }}>
            <SemiBoldHeader style={styles.pageHeader}>
              <label>Institution admin table</label>
              <Button
                type='secondaryLight'
                style={{
                  padding: '0rem',
                  marginTop: '0.45rem',
                  verticalAlign: 'sub',
                }}
                onClick={() =>
                  this.props.navigateByUrl('admin/institution/add')
                }
                data-test-id='add-institution'
              >
                <ClrIcon shape='plus-circle' class='is-solid' size={20} />
              </Button>
            </SemiBoldHeader>
            {institutionLoadError && (
              <div style={{ color: colors.danger }}>
                Error while loading Institution. Please try again later
              </div>
            )}
            <DataTable
              data-test-id='institution-datatable'
              value={institutions}
              paginator={true}
              breakpoint='0px'
              rows={500}
              scrollable={true}
              frozenWidth='10.5rem'
              loading={loadingInstitutions}
              paginatorTemplate='CurrentPageReport FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink  RowsPerPageDropdown'
              currentPageReportTemplate='Showing {first} to {last} of {totalRecords} entries'
            >
              <Column
                field='displayName'
                header='Institution Name'
                body={this.renderInstitutionName}
                bodyStyle={styles.text}
                headerStyle={styles.header}
                frozen={true}
              />
              <Column
                field='organizationTypeEnum'
                header='Institution Type'
                body={this.renderOrganizationType}
                bodyStyle={styles.text}
                headerStyle={styles.header}
              />
              <Column
                field='accessTiers'
                header='Data access tiers'
                body={this.renderAccessTiers}
                bodyStyle={styles.text}
                headerStyle={styles.header}
              />
              <Column
                field='userInstructions'
                header='User Email Instruction'
                body={this.renderInstitutionInstructions}
                bodyStyle={styles.text}
                headerStyle={{ ...styles.header, width: '7.5rem' }}
              />
              <Column
                field='requestAccessUrl'
                header='Custom Request Access URL'
                body={this.renderRequestAccessUrl}
                bodyStyle={styles.text}
                headerStyle={{ ...styles.header, width: '7.5rem' }}
              />
            </DataTable>
          </FadeBox>
        </div>
      );
    }
  }
);
