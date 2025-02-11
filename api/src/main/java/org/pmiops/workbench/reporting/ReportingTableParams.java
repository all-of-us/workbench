package org.pmiops.workbench.reporting;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import org.pmiops.workbench.model.ReportingBase;
import org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer;

public record ReportingTableParams<T extends ReportingBase>(
    String bqTableName,
    int batchSize,
    InsertAllRequestPayloadTransformer<T> bqInsertionBuilder,
    BiFunction<Long, Long, List<T>> rwbBatchQueryFn,
    IntSupplier rwbTableCountFn) {}
