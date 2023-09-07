import * as React from 'react';
import { useEffect, useState } from 'react';

import { CohortDefinition, SearchGroup } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { Spinner } from 'app/components/spinners';
import { searchRequestStore } from 'app/pages/data/cohort/search-state.service';
import { mapGroup } from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { withCurrentWorkspace } from 'app/utils';
import { currentGroupCountsStore } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';

export const CustomFunnel = withCurrentWorkspace()(
  ({
    onExit,
    totalCount,
    workspace: { namespace, id },
  }: {
    onExit: (e: Event) => void;
    totalCount: number;
    workspace: WorkspaceData;
  }) => {
    const [funnelGroups, setFunnelGroups] = useState([]);
    const [searchGroups, setSearchGroups] = useState<SearchGroup[]>([]);

    useEffect(() => {
      if (searchRequestStore.getValue()?.includes?.length > 0) {
        const groupCounts = currentGroupCountsStore.getValue();
        groupCounts.sort((a, b) => b.groupCount - a.groupCount);
        setFunnelGroups(
          groupCounts.map((groupCount, index) => ({
            loading: index > 0,
            count: index === 0 ? groupCount.groupCount : null,
            name: index === 0 ? groupCount.groupName : null,
            groupId: index === 0 ? groupCount.groupId : null,
          }))
        );
        setSearchGroups(searchRequestStore.getValue().includes);
      }
    }, []);

    const getGroupNameFromStore = (groupId: string) =>
      currentGroupCountsStore
        .getValue()
        ?.find((group) => group.groupId === groupId)?.groupName;

    const getCounts = async () => {
      // Split search groups by those that have and have not been added to the funnelGroups array
      const [addedGroups, remainingGroups] = searchGroups.reduce(
        (acc, group) => {
          if (funnelGroups.some((fg) => fg.groupId === group.id)) {
            acc[0].push(group);
          } else {
            acc[1].push(group);
          }
          return acc;
        },
        [[], []]
      );
      if (remainingGroups.length > 1) {
        // If more than one group remaining, call for the intersection of each group with the previously added groups
        const intersectCounts = await Promise.all(
          remainingGroups.map((group) => {
            const searchRequest: CohortDefinition = {
              includes: [...addedGroups.map(mapGroup), mapGroup(group)],
              excludes: [],
              dataFilters: [],
            };
            return cohortBuilderApi().countParticipants(
              namespace,
              id,
              searchRequest
            );
          })
        );
        // Get the index of the highest intersection count
        const highestCountIndex = intersectCounts.indexOf(
          Math.max(...intersectCounts)
        );
        // Update funnelGroups with the group with the highest intersection count
        setFunnelGroups((prevState) => {
          prevState[addedGroups.length] = {
            loading: false,
            count: intersectCounts[highestCountIndex],
            name:
              remainingGroups[highestCountIndex].name ??
              getGroupNameFromStore(remainingGroups[highestCountIndex].id),
            groupId: remainingGroups[highestCountIndex].id,
          };
          return [...prevState];
        });
      } else if (remainingGroups[0]) {
        // Only one group remains to be added, update funnelGroups using the totalCount instead of calling api
        setFunnelGroups((prevState) => {
          prevState[prevState.length - 1] = {
            loading: false,
            count: totalCount,
            name:
              remainingGroups[0].name ??
              getGroupNameFromStore(remainingGroups[0].id),
            groupId: remainingGroups[0].id,
          };
          return [...prevState];
        });
      }
    };

    useEffect(() => {
      if (funnelGroups.length > 2 && funnelGroups.some((fg) => fg.loading)) {
        getCounts();
      }
    }, [funnelGroups]);

    return (
      <>
        <div style={{ height: '1.25rem', width: '100%' }}>
          <Button
            type='link'
            onClick={(e) => onExit(e)}
            style={{
              height: '1.25rem',
              fontSize: '12px',
              float: 'right',
              padding: 0,
            }}
          >
            Close
          </Button>
        </div>
        <div style={{ display: 'flex', width: '100%' }}>
          <div style={{ flex: 1 }}>
            {funnelGroups.map((funnelGroup, index) => (
              <div
                key={index}
                style={{ height: '3rem', paddingTop: '0.25rem' }}
              >
                <h3 style={{ marginTop: 0 }}>{funnelGroup.name}</h3>
              </div>
            ))}
          </div>
          <div style={{ flex: 7 }}>
            {funnelGroups.map((funnelGroup, index) => {
              const percentage =
                (funnelGroup.count / funnelGroups[0].count) * 100;
              return (
                <div
                  key={index}
                  style={{
                    height: '3rem',
                    position: 'relative',
                    textAlign: 'center',
                  }}
                >
                  {funnelGroup.loading ? (
                    <Spinner size={30} style={{ marginTop: '0.25rem' }} />
                  ) : (
                    <>
                      {percentage < 10 && (
                        <div
                          style={{
                            position: 'absolute',
                            left: `calc(${47 - percentage}% - 0.5rem)`,
                            top: '0.5rem',
                          }}
                        >
                          {funnelGroup.count.toLocaleString()}
                        </div>
                      )}
                      <div
                        style={{
                          background: colors.chartColors[index],
                          color: 'white',
                          height: '100%',
                          margin: '0 auto',
                          paddingTop: '0.5rem',
                          textAlign: 'center',
                          width: `${percentage}%`,
                        }}
                      >
                        {percentage >= 10 && funnelGroup.count.toLocaleString()}
                      </div>
                    </>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </>
    );
  }
);
