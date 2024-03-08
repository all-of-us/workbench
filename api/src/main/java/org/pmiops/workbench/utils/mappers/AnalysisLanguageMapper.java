package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.model.AnalysisLanguage;
import org.pmiops.workbench.model.KernelTypeEnum;

@Mapper(config = MapStructConfig.class)
public interface AnalysisLanguageMapper {
  AnalysisLanguage kernelTypeToAnalysisLanguage(KernelTypeEnum kernelType);

  @ValueMapping(source = "SAS", target = MappingConstants.NULL) // not a valid Jupyter kernel type
  KernelTypeEnum analysisLanguageToKernelType(AnalysisLanguage analysisLanguage);
}
