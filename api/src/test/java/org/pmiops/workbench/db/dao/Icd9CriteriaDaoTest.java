package org.pmiops.workbench.db.dao;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.Icd9CriteriaDao;
import org.pmiops.workbench.cdr.model.Icd9Criteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@EntityScan(basePackageClasses = {Icd9Criteria.class})
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class Icd9CriteriaDaoTest {

    @Autowired
    Icd9CriteriaDao icd9CriteriaDao;

    @Test
    @Sql(statements="SET SCHEMA cdr")
    public void findIcd9CriteriaByParentId() throws Exception {
        assertEquals("", icd9CriteriaDao.findIcd9CriteriaByParentId(0L));
    }

}
