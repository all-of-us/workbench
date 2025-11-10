import * as React from 'react';
import { Galleria } from 'primereact/galleria';
import { faUpRightFromSquare } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { environment } from 'environments/environment';
import { Button, StyledExternalLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import { AoU } from 'app/components/text-wrappers';
import colors from 'app/styles/colors';
import vwbCarousel1 from 'assets/images/vwb-carousel-1.png';
import vwbCarousel2 from 'assets/images/vwb-carousel-2.png';
import vwbCarousel3 from 'assets/images/vwb-carousel-3.png';

const VERILY_PRE_URL = 'https://verily.com/solutions/pre-platform';
const VWB_USER_SUPPORT_HUB_URL =
  'https://support.researchallofus.org/hc/en-us/articles/39985865987732-What-to-expect-during-the-Researcher-Workbench-Migration';
const VWB_CAROUSEL_ITEMS = [vwbCarousel1, vwbCarousel2, vwbCarousel3];
const carouselTemplate = (img) => (
  <img src={img} alt='vwb-carousel' style={{ width: '100%' }} />
);

export const VwbBanner = () => {
  return (
    <FlexRow
      style={{
        background: colors.banner,
        borderRadius: '0.5rem',
        margin: '1.5rem 3% 0 3%',
        padding: '1.5rem',
        minWidth: '1450px',
      }}
    >
      <FlexColumn style={{ color: colors.primary, flex: 1.15 }}>
        <div>
          <Header
            style={{
              fontWeight: 600,
              fontSize: '32px',
              lineHeight: '40px',
              margin: 0,
            }}
          >
            A more powerful way to work with <AoU /> data
          </Header>
          <SmallHeader style={{ marginTop: '1.5rem' }}>
            The new{' '}
            <span style={{ fontWeight: 700 }}>
              {' '}
              <AoU /> Researcher experience, built on{' '}
              <StyledExternalLink
                href={VERILY_PRE_URL}
                style={{ color: colors.primary, textDecoration: 'underline' }}
                target='_blank'
              >
                Verily Pre
              </StyledExternalLink>
            </span>{' '}
            offers expanded data exploration and analysis tools, an updated
            interface, and access to enhanced cloud capabilities.
            <ul style={{ marginTop: '1.5rem', paddingLeft: '1.5rem' }}>
              <li>
                You can explore the updated Workbench now and get early access
                to new tools
              </li>
              <li>
                In the coming months, researchers will gradually transition to
                the new Workbench
              </li>
              <li>
                Until then, the legacy environment remains available for you to
                use
              </li>
              <li>
                For details on the update and transition timeline, visit the{' '}
                <StyledExternalLink
                  href={VWB_USER_SUPPORT_HUB_URL}
                  style={{ color: colors.primary, textDecoration: 'underline' }}
                  target='_blank'
                >
                  User Support Hub
                </StyledExternalLink>
              </li>
            </ul>
          </SmallHeader>
          <Button style={{ marginTop: '1rem', height: '45px', width: '300px' }}>
            <a
              href={environment.vwbUiUrl}
              target='_blank'
              style={{ color: colors.white }}
            >
              Explore the Verily Platform
              <FontAwesomeIcon
                icon={faUpRightFromSquare}
                title='Explore the Verily Platform'
                style={{ marginLeft: '0.5rem' }}
              />
            </a>
          </Button>
        </div>
      </FlexColumn>
      <FlexColumn style={{ marginLeft: '1.5rem', flex: 1 }}>
        <Galleria
          value={VWB_CAROUSEL_ITEMS}
          showIndicators
          showIndicatorsOnItem
          showThumbnails={false}
          item={carouselTemplate}
        />
      </FlexColumn>
    </FlexRow>
  );
};
