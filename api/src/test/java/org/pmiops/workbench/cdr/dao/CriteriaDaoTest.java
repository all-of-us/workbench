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

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestCdrJpaConfig.class})
@ActiveProfiles("test-cdr")
public class CriteriaDaoTest {

    @Autowired
    CriteriaDao criteriaDao;
    private Criteria icd9Criteria;
    private Criteria demoCriteria;

    @Before
    public void setUp() {
        icd9Criteria = createCriteria("ICD9");
        demoCriteria = createCriteria("DEMO");
        criteriaDao.save(icd9Criteria);
        criteriaDao.save(demoCriteria);
    }

    @Test
    public void findCriteriaByParentId() throws Exception {
        assertEquals(icd9Criteria, criteriaDao.findCriteriaByTypeLikeAndParentId(icd9Criteria.getType(),0L).get(0));
        assertEquals(demoCriteria, criteriaDao.findCriteriaByTypeLikeAndParentId(demoCriteria.getType(),0L).get(0));
    }

    private Criteria createCriteria(String type) {
        return new Criteria()
                .sortOrder(1)
                .code("002")
                .count("10")
                .conceptId("1000")
                .domainId("Condition")
                .group(false)
                .selectable(false)
                .name("name")
                .parentId(0)
                .type(type);
    }

}
