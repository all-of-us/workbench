import { useEffect, useState } from 'react';

import { environment } from 'environments/environment';
import { Button } from 'app/components/buttons';
import { userApi, workspacesApi } from 'app/services/swagger-fetch-clients';
import { withCurrentWorkspace } from 'app/utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import { VwbImportantBanner } from './vwb-migration-banner';
import { VwbMigrationInfoBox } from './vwb-migration-infobox';

interface Props {
  workspace: WorkspaceData;
}

export const MigrationPage = withCurrentWorkspace()(({ workspace }: Props) => {
  const [selectedPod, setSelectedPod] = useState('');
  const [pods, setPods] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasAcceptedTos, setHasAcceptedTos] = useState<boolean | null>(null);

  if (!workspace) {
    return null;
  }

  // Load Pods
  useEffect(() => {
    const loadPods = async () => {
      try {
        const response = await workspacesApi().getUserPods();
        setPods(response || []);
      } catch (e) {
        console.error('Failed to load pods', e);
      }
    };

    loadPods();
  }, []);

  // Start Migration
  const handleMigration = async () => {
    try {
      setLoading(true);

      await workspacesApi().startWorkspaceMigration(
        workspace.namespace,
        workspace.terraName,
        {
          folders: [], // keep empty for now
          podId: selectedPod,
          researchPurpose: JSON.stringify(workspace.researchPurpose),
        }
      );

      console.log('Migration started');
    } catch (e) {
      console.error('Migration failed', e);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    const fetchTos = async () => {
      try {
        const res = await userApi().getUserTosStatus();
        setHasAcceptedTos(res);
      } catch (e) {
        console.error('Failed to fetch ToS state', e);
        setHasAcceptedTos(false); // safe fallback
      }
    };

    fetchTos();
  }, []);
  console.log(hasAcceptedTos);
  return (
    <div style={{ padding: '1.5rem 2rem' }}>
      {/* 🔶 TOP BANNER */}
      {!hasAcceptedTos && (
        <VwbImportantBanner
          title='Important'
          message='Before starting migration, log into Researcher Workbench 2.0 to agree to the terms of service. You only need to do this once.'
          actionText='Open Verily Workbench'
          onAction={() => window.open(environment.vwbUiUrl, '_blank')}
        />
      )}
      <VwbMigrationInfoBox />
      {/* 🧾 WORKSPACE ROW */}
      <div
        style={{
          border: '1px solid #D3DAE6',
          borderRadius: '8px',
          padding: '16px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        {/* Left Info */}
        <div>
          <div style={{ fontWeight: 600 }}>{workspace.terraName}</div>
          <div style={{ fontSize: '13px', color: '#6B7280' }}>
            Controlled Tier Dataset
          </div>
        </div>

        {/* Pod Select */}
        <div style={{ width: '300px' }}>
          <select
            value={selectedPod}
            onChange={(e) => setSelectedPod(e.target.value)}
            style={{
              width: '100%',
              padding: '8px',
              borderRadius: '6px',
              border: '1px solid #D3DAE6',
            }}
          >
            <option value=''>Select a pod</option>
            {pods.map((pod) => (
              <option key={pod.podId} value={pod.podId}>
                {pod.userFacingId || pod.description || pod.podId}
              </option>
            ))}
          </select>
        </div>

        {/* CTA */}
        <Button disabled={!selectedPod || loading} onClick={handleMigration}>
          {loading ? 'Starting...' : 'Start migration'}
        </Button>
      </div>
    </div>
  );
});
