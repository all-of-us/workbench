package org.pmiops.workbench.cdr.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class DbDomainDaoTest {

    @Autowired
    DbDomainDao dao;
    private DbDomain obj1;
    private DbDomain obj2;
    private DbDomain obj3;
    private DbDomain obj4;
    private DbDomain obj5;

    @Before
    public void setUp() {

        obj1 = createDbDomain("Domain1","Sample Domain");
        obj2 = createDbDomain("Domain2","Sample Domain");
        obj3 = createDbDomain("Domain3","Sample Domain");
        obj4 = createDbDomain("Domain4","Sample Domain");
        obj5 = createDbDomain("Domain5","Sample Domain");

        dao.save(obj1);
        dao.save(obj2);
        dao.save(obj3);
        dao.save(obj4);
        dao.save(obj5);
    }

    @Test
    public void findAllDbDomains() throws Exception {
        /* Todo write more tests */
        final List<DbDomain> list = dao.findAll();
        assert(obj1.getDomainId().equals("Domain1"));
        assert(list.get(0).getDomainId().equals(obj1.getDomainId()));
    }

    @Test
    public void findDbDomainsByDbType() throws Exception {
        /* Todo write more tests */
        final List<DbDomain> list = dao.findByDbType("Sample Domain");
        assert(obj1.getDomainId().equals("Domain1"));
        assert(list.get(0).getDomainId().equals(obj1.getDomainId()));
    }

    @Test
    public void findDbDomainsByDbTypeAndConceptId() throws Exception {
        /* Todo write more tests */
        final List<DbDomain> list = dao.findByDbTypeAndAndConceptIdNotNull("Sample Domain");
        assert(obj1.getDomainId().equals("Domain1"));
        assert(list.get(0).getDomainId().equals(obj1.getDomainId()));
    }

    private DbDomain createDbDomain(String domainId, String dbType) {
        return new DbDomain()
                .domainId(domainId)
                .domainDisplay("Domain description for display")
                .domainDesc("Domain description")
                .dbType(dbType)
                .domainRoute("Domain Route")
                .conceptId(Long.valueOf(0))
                .countValue(Long.valueOf(0));
    }

}
