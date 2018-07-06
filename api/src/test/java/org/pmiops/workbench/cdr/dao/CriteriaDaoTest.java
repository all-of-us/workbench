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

    private static final String TYPE_ICD9 = "ICD9";
    private static final String TYPE_ICD10 = "ICD10";
    private static final String TYPE_CPT = "CPT";
    private static final String TYPE_DEMO = "DEMO";
    private static final String SUBTYPE_NONE = null;
    private static final String SUBTYPE_ICD10PCS = "ICD10PCS";
    private static final String SUBTYPE_RACE = "RACE";
    private static final String SUBTYPE_AGE = "AGE";

    @Autowired
    CriteriaDao criteriaDao;
    private Criteria icd9Criteria1;
    private Criteria icd9Criteria2;
    private Criteria demoCriteria1;
    private Criteria demoCriteria1a;
    private Criteria demoCriteria2;
    private Criteria icd10Criteria1;
    private Criteria icd10Criteria2;
    private Criteria cptCriteria1;
    private Criteria cptCriteria2;
    private Criteria parentIcd9;
    private Criteria childIcd9;
    private Criteria parentDemo;
    private Criteria parentIcd10;
    private Criteria childIcd10;

    @Before
    public void setUp() {
        icd9Criteria1 = createCriteria(TYPE_ICD9, SUBTYPE_NONE, "002", "blah chol", 0, false, true);
        icd9Criteria2 = createCriteria(TYPE_ICD9, SUBTYPE_NONE, "001", "chol blah", 0, false, true);
        parentDemo = createCriteria(TYPE_DEMO, SUBTYPE_RACE, "Race/Ethnicity", "Race/Ethnicity", 0, true, true);
        demoCriteria1 = createCriteria(TYPE_DEMO, SUBTYPE_RACE, "AF", "African", parentDemo.getId(), false, true);
        demoCriteria1a = createCriteria(TYPE_DEMO, SUBTYPE_RACE, "B", "African American", parentDemo.getId(), false, true);
        demoCriteria2 = createCriteria(TYPE_DEMO, SUBTYPE_AGE, "Age", "demo age", 0, false, true);
        icd10Criteria1 = createCriteria(TYPE_ICD10, SUBTYPE_NONE, "002", "icd10 test 1", 0, false, true);
        icd10Criteria2 = createCriteria(TYPE_ICD10, SUBTYPE_NONE, "001", "icd10 test 2", 0, false, true);
        cptCriteria1 = createCriteria(TYPE_CPT, SUBTYPE_NONE, "0039T", "zzzcptzzz", 0, false, true);
        cptCriteria2 = createCriteria(TYPE_CPT, SUBTYPE_NONE, "0001T", "zzzCPTxxx", 0, false, true);
        parentIcd9 = createCriteria(TYPE_ICD9, SUBTYPE_NONE, "003", "name", 0, true, true);
        parentIcd10 = createCriteria(TYPE_ICD10, SUBTYPE_ICD10PCS, "003", "name", 0, true, true);

        criteriaDao.save(icd9Criteria1);
        criteriaDao.save(icd9Criteria2);
        criteriaDao.save(parentDemo);
        criteriaDao.save(demoCriteria1);
        criteriaDao.save(demoCriteria1a);
        criteriaDao.save(demoCriteria2);
        criteriaDao.save(icd10Criteria1);
        criteriaDao.save(icd10Criteria2);
        criteriaDao.save(cptCriteria1);
        criteriaDao.save(cptCriteria2);
        criteriaDao.save(parentIcd9);
        criteriaDao.save(parentIcd10);
        childIcd9 = createCriteria(TYPE_ICD9, SUBTYPE_NONE, "003.1", "name", parentIcd9.getId(), false, true);
        criteriaDao.save(childIcd9);
        childIcd10 = createCriteria(TYPE_ICD10, SUBTYPE_ICD10PCS, "003.1", "name", parentIcd10.getId(), false, true);
        criteriaDao.save(childIcd10);
    }

    @After
    public void tearDown() {
        criteriaDao.delete(icd9Criteria1);
        criteriaDao.delete(icd9Criteria2);
        criteriaDao.delete(demoCriteria1);
        criteriaDao.delete(demoCriteria1a);
        criteriaDao.delete(demoCriteria2);
        criteriaDao.delete(icd10Criteria1);
        criteriaDao.delete(icd10Criteria2);
        criteriaDao.delete(cptCriteria1);
        criteriaDao.delete(cptCriteria2);
        criteriaDao.delete(parentIcd9);
        criteriaDao.delete(childIcd9);
        criteriaDao.delete(parentDemo);
        criteriaDao.delete(parentIcd10);
        criteriaDao.delete(childIcd10);
    }

    @Test
    public void findCriteriaByParentId() throws Exception {
        final List<Criteria> icd9List = criteriaDao.findCriteriaByTypeAndParentIdOrderByCodeAsc(TYPE_ICD9, 0L);
        assertEquals(icd9Criteria2, icd9List.get(0));
        assertEquals(icd9Criteria1, icd9List.get(1));

        final List<Criteria> icd10List = criteriaDao.findCriteriaByTypeAndParentIdOrderByCodeAsc(TYPE_ICD10, 0L);
        assertEquals(icd10Criteria2, icd10List.get(0));
        assertEquals(icd10Criteria1, icd10List.get(1));

        final List<Criteria> cptList = criteriaDao.findCriteriaByTypeAndParentIdOrderByCodeAsc(TYPE_CPT, 0L);
        assertEquals(cptCriteria2, cptList.get(0));
        assertEquals(cptCriteria1, cptList.get(1));
    }

    @Test
    public void findCriteriaByTypeAndSubtypeOrderByNameAsc() throws Exception {
        final List<Criteria> demoList = criteriaDao.findCriteriaByTypeAndSubtypeOrderByNameAsc(TYPE_DEMO, SUBTYPE_RACE);
        assertEquals(2, demoList.size());
        assertEquals(demoCriteria1, demoList.get(0));
        assertEquals(demoCriteria1a, demoList.get(1));
    }

    @Test
    public void findCriteriaByTypeAndCode() throws Exception {
        final List<String> icd9DomainList = criteriaDao.findCriteriaByTypeAndCode(TYPE_ICD9, "003");

        assertEquals(1, icd9DomainList.size());
        assertEquals("Condition", icd9DomainList.get(0));
    }

    @Test
    public void findCriteriaByTypeAndSubtypeAndCode() throws Exception {
        final List<String> icd10DomainList = criteriaDao.findCriteriaByTypeAndSubtypeAndCode(TYPE_ICD10, SUBTYPE_ICD10PCS, "003");

        assertEquals(1, icd10DomainList.size());
        assertEquals("Condition", icd10DomainList.get(0));
    }

    private Criteria createCriteria(String type, String subtype, String code, String name, long parentId, boolean group, boolean selectable) {
        return new Criteria()
                .code(code)
                .count("10")
                .conceptId("1000")
                .domainId("Condition")
                .group(group)
                .selectable(selectable)
                .name(name)
                .parentId(parentId)
                .type(type)
                .subtype(subtype);
    }

}
