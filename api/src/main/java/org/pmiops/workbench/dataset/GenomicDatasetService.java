package org.pmiops.workbench.dataset;

import java.util.List;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.TanagraGenomicDataRequest;

public interface GenomicDatasetService {

  List<String> getPersonIdsWithWholeGenome(DbDataset dataSet);

  List<String> getTanagraPersonIdsWithWholeGenome(
      DbWorkspace workspace, TanagraGenomicDataRequest tanagraGenomicDataRequest);
}
