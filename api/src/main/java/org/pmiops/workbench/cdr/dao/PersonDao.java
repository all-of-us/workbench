package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.DbPerson;
import org.pmiops.workbench.model.AgeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface PersonDao extends CrudRepository<DbPerson, Long> {

  @Query(
      value =
          "SELECT count(person_id)\n"
              + "from cb_person\n"
              + "where DATE_FORMAT(NOW(), '%Y') - DATE_FORMAT(dob, '%Y') - (DATE_FORMAT(NOW(), '00-%m-%d') < DATE_FORMAT(dob, '00-%m-%d')) between :startAge and :endAge",
      nativeQuery = true)
  long countByAge(@Param("startAge") int startAge, @Param("endAge") int endAge);

  long countByAgeAtConsentBetween(int startAge, int endAge);

  long countByAgeAtCdrBetween(int startAge, int endAge);

  default long countAgesByType(AgeType ageType, int startAge, int endAge) {
    if (AgeType.AGE.equals(ageType)) {
      return countByAge(startAge, endAge);
    } else if (AgeType.CONSENT.equals(ageType)) {
      return countByAgeAtConsentBetween(startAge, endAge);
    }
    return countByAgeAtCdrBetween(startAge, endAge);
  }
}
