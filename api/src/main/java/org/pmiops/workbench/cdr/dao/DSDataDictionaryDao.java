package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface DSDataDictionaryDao extends CrudRepository<DbDSDataDictionary, Long> {
  @Query(
      value =
          "select * from \n"
              + "(\n"
              + "select * \n"
              + "from ds_data_dictionary \n"
              + "where field_name = :name \n"
              + "and domain = :domain\n"
              + "and relevant_omop_table not like 'ds_%'\n"
              + "UNION\n"
              + "select * \n"
              + "from ds_data_dictionary \n"
              + "where field_name = :name \n"
              + "and domain = :domain\n"
              + "and relevant_omop_table like 'ds_%') d limit 1",
      nativeQuery = true)
  DbDSDataDictionary findFirstByFieldNameAndDomain(
      @Param("name") String name, @Param("domain") String domain);
}
