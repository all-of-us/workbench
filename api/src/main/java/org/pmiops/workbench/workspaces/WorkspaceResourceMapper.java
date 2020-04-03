package org.pmiops.workbench.workspaces;

import org.mapstruct.Mapper;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface WorkspaceResourceMapper {}
