package org.pmiops.workbench.utils.mappers;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    suppressTimestampInGenerated = true)
public class MapStructConfig {}
