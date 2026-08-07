import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { DATA_ACCESS_REQUIREMENTS_PATH } from 'app/utils/access-utils';
import { useNavigation } from 'app/utils/navigation';
import { profileStore, serverConfigStore, useStore } from 'app/utils/stores';
import migrationEnded from 'assets/images/migration-ended.jpg';

export const shouldShowDuccUpdateBanner = (
  duccSignedVersion: number | undefined,
  latestDuccVersion: number | undefined,
  currentDuccVersions: number[] = []
): boolean => {
  const fallbackLatestVersion =
    currentDuccVersions.length > 0
      ? Math.max(...currentDuccVersions)
      : undefined;
  const resolvedLatestVersion = latestDuccVersion ?? fallbackLatestVersion;

  return (
    typeof duccSignedVersion === 'number' &&
    typeof resolvedLatestVersion === 'number' &&
    duccSignedVersion < resolvedLatestVersion
  );
};

const styles = reactStyles({
  banner: {
    background: colors.banner,
    borderRadius: '0.5rem',
    margin: '1.5rem 3% 0 3%',
    padding: '1rem',
    height: '450px',
    width: '1350px',
  },
  textColumn: {
    color: colors.primary,
    flex: 1,
  },
  title: {
    fontSize: '32px',
    fontWeight: 600,
    lineHeight: '40px',
    margin: 0,
  },
  body: {
    marginTop: '1rem',
  },
  buttonRow: {
    marginTop: '2rem',
    alignItems: 'center',
  },
  reviewButton: {
    height: '45px',
    padding: '0 1rem',
    whiteSpace: 'nowrap',
  },
  imageColumn: {
    marginLeft: '1.5rem',
    flex: 1.5,
  },
  image: {
    marginTop: '1%',
    width: '100%',
    height: '95%',
    objectFit: 'contain',
  },
});

/**
 * Displays an action-required banner when the signed-in user has not yet signed
 * the current version of the Data User Code of Conduct (DUCC).
 *
 * Visibility: shown when !isCurrentDUCCVersion(profile.duccSignedVersion)
 * Renders null when the user is current or while profile is loading.
 */
export const DuccUpdateBanner = () => {
  const { profile } = useStore(profileStore);
  const { config } = useStore(serverConfigStore);
  const [navigate] = useNavigation();
  const showBanner = shouldShowDuccUpdateBanner(
    profile?.duccSignedVersion,
    config?.latestDuccVersion,
    config?.currentDuccVersions || []
  );

  // Hide while profile/config are loading or user is already on latest DUCC.
  if (!showBanner) {
    return null;
  }

  return (
    <FlexRow role='alert' aria-label='Action required: updated Data User Code of Conduct' style={styles.banner}>
      <FlexColumn style={styles.textColumn}>
        <div>
          <Header style={styles.title}>Updated Data User Code of Conduct Available</Header>

          <SmallHeader style={styles.body}>
            A revised Data User Code of Conduct is available. Please review and sign the updated
            agreement.
          </SmallHeader>

          <FlexRow style={styles.buttonRow}>
            <Button
              aria-label='Review updated Data User Code of Conduct'
              style={styles.reviewButton}
              onClick={() => navigate([DATA_ACCESS_REQUIREMENTS_PATH])}
            >
              Review DUCC
            </Button>
          </FlexRow>
        </div>
      </FlexColumn>

      <FlexColumn style={styles.imageColumn}>
        <img src={migrationEnded} alt='Homepage notification illustration' style={styles.image} />
      </FlexColumn>
    </FlexRow>
  );
};

