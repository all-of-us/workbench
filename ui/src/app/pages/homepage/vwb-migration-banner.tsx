import { faUpRightFromSquare } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { environment } from 'environments/environment';
import { Button, StyledExternalLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import colors from 'app/styles/colors';
import { useNavigation } from 'app/utils/navigation';
import vwbMigrationImage from 'assets/images/vwb-migration.png';

const VWB_USER_SUPPORT_HUB_URL =
  'https://support.researchallofus.org/hc/en-us/articles/48266066855188';
const VWB_USER_BILLING_POD_URL =
  'https://support.researchallofus.org/hc/en-us/articles/41981050556564-' +
  'Getting-Started-in-new-Researcher-Workbench-2-0#h_01KDR2615S4SGFD62K5MJ4VKPA';

export const VwbMigrationBanner = () => {
  const [navigate] = useNavigation();
  return (
    <FlexRow
      style={{
        background: colors.banner,
        margin: '1.5rem 3% 0 3%',
        padding: '1rem',
        height: '450px',
        width: '1350px',
      }}
    >
      {/* LEFT CONTENT */}
      <FlexColumn style={{ color: colors.primary, flex: 1 }}>
        <div>
          {/* TITLE */}
          <Header
            style={{
              fontWeight: 600,
              fontSize: '24px',
              lineHeight: '32px',
              margin: 0,
            }}
          >
            Migrate your Workspaces to Researcher Workbench 2.0
          </Header>

          {/* DESCRIPTION */}
          <SmallHeader
            style={{
              fontSize: '14px',
              lineHeight: '1.25rem',
              marginTop: '1rem',
            }}
          >
            You are now able to migrate your workspaces to Researcher Workbench
            2.0, powered on Verily Pre. Review your current workspaces to decide
            which ones you want to migrate. Any workspaces you don’t migrate
            will be archived. Any workspace associated with CDR v7 and v8 can be
            migrated. Review the detailed migration instructions and tips on the{' '}
            User Support Hub {'  '}
            <StyledExternalLink
              href={VWB_USER_SUPPORT_HUB_URL}
              style={{ color: colors.primary, textDecoration: 'underline' }}
              target='_blank'
            >
              Learn more
            </StyledExternalLink>
          </SmallHeader>

          {/* PRE-REQUISITES */}
          <div
            style={{
              fontSize: '14px',
              lineHeight: '1.25rem',
              marginTop: '1rem',
            }}
          >
            <SmallHeader style={{ fontWeight: 600 }}>
              Before you begin workspace migration, you must:
            </SmallHeader>

            <ol style={{ marginTop: 0, paddingLeft: '1.5rem' }}>
              {/* STEP 1 */}
              <li style={{ marginBottom: 0 }}>
                Log into Researcher Workbench 2.0 to accept the terms of
                service.{' '}
                <StyledExternalLink
                  href={environment.vwbUiUrl}
                  target='_blank'
                  style={{ color: colors.primary, textDecoration: 'underline' }}
                >
                  Login here
                </StyledExternalLink>
              </li>

              {/* STEP 2 */}
              <li>
                Set up billing in Researcher Workbench 2.0
                <div style={{ marginTop: '0.5rem' }}>
                  If you still have initial credits, you can use them in the new
                  Workbench. Otherwise you must set up a billing pod before you
                  begin migration.{' '}
                  <StyledExternalLink
                    href={VWB_USER_BILLING_POD_URL}
                    target='_blank'
                    style={{
                      color: colors.primary,
                      textDecoration: 'underline',
                    }}
                  >
                    Learn more
                  </StyledExternalLink>
                </div>
              </li>
            </ol>
          </div>

          {/* CTA BUTTON */}
          <Button
            style={{ marginTop: '0.5rem', height: '45px', width: '300px' }}
            onClick={() => navigate(['workspaces'])}
          >
            Go to My Workspaces
            <FontAwesomeIcon
              icon={faUpRightFromSquare}
              style={{ marginLeft: '0.5rem' }}
            />
          </Button>
        </div>
      </FlexColumn>

      {/* RIGHT CAROUSEL */}
      <FlexColumn style={{ marginLeft: '1.5rem', flex: 1.35 }}>
        <img
          src={vwbMigrationImage}
          alt='Workspace migration'
          style={{
            marginTop: '1%',
            width: '100%',
            height: '95%',
          }}
        />
      </FlexColumn>
    </FlexRow>
  );
};
