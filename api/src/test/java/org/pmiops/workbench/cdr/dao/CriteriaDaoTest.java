package org.pmiops.workbench.cdr.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
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
    private Criteria parentIcd9;
    private Criteria childIcd9;
    private Criteria parentIcd10;
    private Criteria childIcd10;

    @Before
    public void setUp() {
        icd9Criteria1 = createCriteria("ICD9", null, "002", "blah chol");
        icd9Criteria2 = createCriteria("ICD9", null, "001", "chol blah");
        demoCriteria1 = createCriteria("DEMO", "RACE", "Race/Ethnicity", "demo race");
        demoCriteria2 = createCriteria("DEMO", "AGE", "Age", "demo age");
        icd10Criteria1 = createCriteria("ICD10", null, "002", "icd10 test 1");
        icd10Criteria2 = createCriteria("ICD10", null, "001", "icd10 test 2");
        cptCriteria1 = createCriteria("CPT", null, "0039T", "zzzcptzzz");
        cptCriteria2 = createCriteria("CPT", null, "0001T", "zzzCPTxxx");
        parentIcd9 = new Criteria()
                .code("003")
                .count("10")
                .conceptId("1000")
                .domainId(null)
                .group(true)
                .selectable(true)
                .name("name")
                .parentId(0)
                .type("ICD9")
                .subtype(null);
        parentIcd10 = new Criteria()
                .code("003")
                .count("10")
                .conceptId("1000")
                .domainId(null)
                .group(true)
                .selectable(true)
                .name("name")
                .parentId(0)
                .type("ICD10")
                .subtype("ICD10PCS");

        criteriaDao.save(icd9Criteria1);
        criteriaDao.save(icd9Criteria2);
        criteriaDao.save(demoCriteria1);
        criteriaDao.save(demoCriteria2);
        criteriaDao.save(icd10Criteria1);
        criteriaDao.save(icd10Criteria2);
        criteriaDao.save(cptCriteria1);
        criteriaDao.save(cptCriteria2);
        criteriaDao.save(parentIcd9);
        criteriaDao.save(parentIcd10);
        childIcd9 = new Criteria()
                .code("003.1")
                .count("10")
                .conceptId("1000")
                .domainId("Condition")
                .group(false)
                .selectable(true)
                .name("name")
                .parentId(parentIcd9.getId())
                .type("ICD9")
                .subtype(null);
        criteriaDao.save(childIcd9);
        childIcd10 = new Criteria()
                .code("003.1")
                .count("10")
                .conceptId("1000")
                .domainId("Condition")
                .group(false)
                .selectable(true)
                .name("name")
                .parentId(parentIcd10.getId())
                .type("ICD10")
                .subtype("ICD10PCS");
        criteriaDao.save(childIcd10);
    }

    @After
    public void tearDown() {
        criteriaDao.delete(icd9Criteria1);
        criteriaDao.delete(icd9Criteria2);
        criteriaDao.delete(demoCriteria1);
        criteriaDao.delete(demoCriteria2);
        criteriaDao.delete(icd10Criteria1);
        criteriaDao.delete(icd10Criteria2);
        criteriaDao.delete(cptCriteria1);
        criteriaDao.delete(cptCriteria2);
        criteriaDao.delete(parentIcd9);
        criteriaDao.delete(childIcd9);
        criteriaDao.delete(parentIcd10);
        criteriaDao.delete(childIcd10);
    }

    @Test
    public void findCriteriaByParentId() throws Exception {
        final List<Criteria> icd9List = criteriaDao.findCriteriaByTypeAndParentIdOrderByCodeAsc(icd9Criteria1.getType(), 0L);
        assertEquals(icd9Criteria2, icd9List.get(0));
        assertEquals(icd9Criteria1, icd9List.get(1));

        final List<Criteria> demoList = criteriaDao.findCriteriaByTypeAndParentIdOrderByCodeAsc("DEMO", 0L);
        assertEquals(demoCriteria2, demoList.get(0));
        assertEquals(demoCriteria1, demoList.get(1));

        final List<Criteria> icd10List = criteriaDao.findCriteriaByTypeAndParentIdOrderByCodeAsc("ICD10", 0L);
        assertEquals(icd10Criteria2, icd10List.get(0));
        assertEquals(icd10Criteria1, icd10List.get(1));

        final List<Criteria> cptList = criteriaDao.findCriteriaByTypeAndParentIdOrderByCodeAsc("CPT", 0L);
        assertEquals(cptCriteria2, cptList.get(0));
        assertEquals(cptCriteria1, cptList.get(1));
    }

    @Test
    public void findCriteriaByTypeAndCode() throws Exception {
        final List<String> icd9DomainList = criteriaDao.findCriteriaByTypeAndCode("ICD9", "003");

        assertEquals(1, icd9DomainList.size());
        assertEquals("Condition", icd9DomainList.get(0));
    }

    @Test
    public void findCriteriaByTypeAndSubtypeAndCode() throws Exception {
        final List<String> icd10DomainList = criteriaDao.findCriteriaByTypeAndSubtypeAndCode("ICD10", "ICD10PCS", "003");

        assertEquals(1, icd10DomainList.size());
        assertEquals("Condition", icd10DomainList.get(0));
    }

    private Criteria createCriteria(String type, String subtype, String code, String name) {
        return new Criteria()
                .code(code)
                .count("10")
                .conceptId("1000")
                .domainId("Condition")
                .group(false)
                .selectable(true)
                .name(name)
                .parentId(0)
                .type(type)
                .subtype(subtype);
    }

}
