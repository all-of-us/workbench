package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DomainVocabularyInfo;
import org.pmiops.workbench.cdr.model.DomainVocabularyInfo.DomainVocabularyInfoId;
import org.springframework.data.repository.CrudRepository;

public interface DomainVocabularyInfoDao extends CrudRepository<DomainVocabularyInfo, DomainVocabularyInfoId> {

  List<DomainVocabularyInfo> findById_DomainIdOrderById_VocabularyId(String domainId);
}
