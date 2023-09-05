import * as React from 'react';
import { useEffect, useState } from 'react';

import { searchRequestStore } from 'app/pages/data/cohort/search-state.service';
import { currentGroupCountsStore } from 'app/utils/navigation';
import {SearchGroup} from 'generated/fetch';

export const CustomFunnel = () => {
  const [funnelGroups, setFunnelGroups] = useState([]);
  const [searchGroups, setSearchGroups] = useState<SearchGroup[]>([]);

  useEffect(() => {
    if (searchRequestStore.getValue()?.includes?.length > 0) {
      setSearchGroups(searchRequestStore.getValue().includes);
      setFunnelGroups(searchRequestStore.getValue().includes.map((group) => ({
        loading: true,
        count: null,
        name: group.name,
      })));
    }
  });

  const getCounts = async (groups: SearchGroup[], index: number) => {
    if (index === 0) {
      const groupCounts = currentGroupCountsStore.getValue();
      groupCounts.sort((a, b) => a.groupCount + b.groupCount);
      setFunnelGroups((prevState) => {
        prevState[0] = {...prevState[0], loading: false, count: groupCounts[0].groupCount};
        return prevState;
      });
    }
  };

  useEffect(() => {
    if (searchGroups.length > 2) {
      getCounts(searchGroups, 0);
    }
  }, [searchGroups]);

  return (
    <></>
  );
};
