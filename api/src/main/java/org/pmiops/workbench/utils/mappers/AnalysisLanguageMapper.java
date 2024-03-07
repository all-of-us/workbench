package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.pmiops.workbench.model.AnalysisLanguage;
import org.pmiops.workbench.model.KernelTypeEnum;

@Mapper(config = MapStructConfig.class)
public interface AnalysisLanguageMapper {
  KernelTypeEnum analysisLanguageToKernelType(AnalysisLanguage analysisLanguage);
}
