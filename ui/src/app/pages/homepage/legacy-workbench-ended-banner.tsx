import * as React from 'react';
import { faUpRightFromSquare } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { environment } from 'environments/environment';
import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import { AoU } from 'app/components/text-wrappers';
import colors from 'app/styles/colors';
import migrationEnded from 'assets/images/migration-ended.jpg';

const SUPPORT_ARTICLE_URL = 'https://support.researchallofus.org/hc/en-us';

export const LegacyWorkbenchEndedBanner = () => {
  return (
    <FlexRow
      style={{
        background: colors.banner,
        borderRadius: '0.5rem',
        margin: '1.5rem 3% 0 3%',
        padding: '1rem',
        height: '450px',
        width: '1350px',
      }}
    >
      <FlexColumn style={{ color: colors.primary, flex: 1 }}>
        <div>
          <Header
            style={{
              fontWeight: 600,
              fontSize: '32px',
              lineHeight: '40px',
              margin: 0,
            }}
          >
            Migration has ended.
          </Header>

          <SmallHeader style={{ marginTop: '1.5rem' }}>
            All of your migrated workspaces from the Legacy <AoU /> Researcher
            Workbench are now available in Researcher Workbench 2.0.
          </SmallHeader>

          <SmallHeader style={{ marginTop: '1rem' }}>
            To recover a legacy workspace, navigate to the Workspaces page and
            access your archived workspaces to get started.
          </SmallHeader>

          <FlexRow
            style={{
              marginTop: '2rem',
              gap: '1rem',
              alignItems: 'center',
            }}
          >
            <Button
              style={{
                height: '45px',
                padding: '0 1rem',
                whiteSpace: 'nowrap',
              }}
              onClick={() => window.open(environment.vwbUiUrl, '_blank')}
            >
              Open Researcher Workbench
              <FontAwesomeIcon
                icon={faUpRightFromSquare}
                style={{ marginLeft: '0.5rem' }}
              />
            </Button>

            <Button
              type='secondaryOutline'
              style={{
                height: '45px',
                padding: '0 1rem',
                whiteSpace: 'nowrap',
              }}
              onClick={() => window.open(SUPPORT_ARTICLE_URL, '_blank')}
            >
              View User Support Hub
              <FontAwesomeIcon
                icon={faUpRightFromSquare}
                style={{ marginLeft: '0.5rem' }}
              />
            </Button>
          </FlexRow>
        </div>
      </FlexColumn>
      {/* RIGHT IMAGE */}
      <FlexColumn
        style={{
          marginLeft: '1.5rem',
          flex: 1.5,
        }}
      >
        <img
          src={migrationEnded}
          alt='Legacy Workbench Ended'
          style={{
            marginTop: '1%',
            width: '100%',
            height: '95%',
            objectFit: 'contain',
          }}
        />
      </FlexColumn>
    </FlexRow>
  );
};
