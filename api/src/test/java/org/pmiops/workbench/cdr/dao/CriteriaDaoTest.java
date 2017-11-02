package org.pmiops.workbench.cdr.dao;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.testconfig.TestCdrJpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestCdrJpaConfig.class})
@ActiveProfiles("test-cdr")
public class CriteriaDaoTest {

    @Autowired
    CriteriaDao criteriaDao;
    private Criteria icd9Criteria1;
    private Criteria icd9Criteria2;
    private Criteria demoCriteria1;
    private Criteria demoCriteria2;
    private Criteria icd10Criteria1;
    private Criteria icd10Criteria2;
    private Criteria cptCriteria1;
    private Criteria cptCriteria2;

    @Before
    public void setUp() {
        icd9Criteria1 = createCriteria("ICD9", null, "002");
        icd9Criteria2 = createCriteria("ICD9", null, "001");
        demoCriteria1 = createCriteria("DEMO", "RACE", "Race/Ethnicity");
        demoCriteria2 = createCriteria("DEMO", "AGE", "Age");
        icd10Criteria1 = createCriteria("ICD10", null, "002");
        icd10Criteria2 = createCriteria("ICD10", null, "001");
        cptCriteria1 = createCriteria("CPT", null, "0039T");
        cptCriteria2 = createCriteria("CPT", null, "0001T");

        criteriaDao.save(icd9Criteria1);
        criteriaDao.save(icd9Criteria2);
        criteriaDao.save(demoCriteria1);
        criteriaDao.save(demoCriteria2);
        criteriaDao.save(icd10Criteria1);
        criteriaDao.save(icd10Criteria2);
        criteriaDao.save(cptCriteria1);
        criteriaDao.save(cptCriteria2);
    }

    @Test
    public void findCriteriaByParentId() throws Exception {
        final List<Criteria> icd9List = criteriaDao.findCriteriaByTypeAndParentId(icd9Criteria1.getType(), 0L);
        assertEquals(icd9Criteria2, icd9List.get(0));
        assertEquals(icd9Criteria1, icd9List.get(1));

        final List<Criteria> demoList = criteriaDao.findCriteriaByTypeAndParentId("DEMO", 0L);
        assertEquals(demoCriteria2, demoList.get(0));
        assertEquals(demoCriteria1, demoList.get(1));

        final List<Criteria> icd10List = criteriaDao.findCriteriaByTypeAndParentId("ICD10", 0L);
        assertEquals(icd10Criteria2, icd10List.get(0));
        assertEquals(icd10Criteria1, icd10List.get(1));

        final List<Criteria> cptList = criteriaDao.findCriteriaByTypeAndParentId("CPT", 0L);
        assertEquals(cptCriteria2, cptList.get(0));
        assertEquals(cptCriteria1, cptList.get(1));
    }

    private Criteria createCriteria(String type, String subtype, String code) {
        return new Criteria()
                .code(code)
                .count("10")
                .conceptId("1000")
                .domainId("Condition")
                .group(false)
                .selectable(false)
                .name("name")
                .parentId(0)
                .type(type)
                .subtype(subtype);
    }

}
