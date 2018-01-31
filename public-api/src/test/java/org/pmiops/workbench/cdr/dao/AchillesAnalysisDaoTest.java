package org.pmiops.workbench.cdr.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
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
public class AchillesAnalysisDaoTest {

    @Autowired
    AchillesAnalysisDao dao;
    private AchillesAnalysis obj1;
    private AchillesAnalysis obj2;
    private AchillesAnalysis obj3;
    private AchillesAnalysis obj4;
    private AchillesAnalysis obj5;

    @Before
    public void setUp() {

        obj1 = createAnalysis(1, "Analysis 1");
        obj2 = createAnalysis(2, "Analysis 2");
        obj3 = createAnalysis(3, "Analysis 3");
        obj4 = createAnalysis(4, "Analysis 4");
        obj5 = createAnalysis(5, "Analysis 5");

        dao.save(obj1);
        dao.save(obj2);
        dao.save(obj3);
        dao.save(obj4);
        dao.save(obj5);
    }

    @Test
    public void findAllAnalyses() throws Exception {
        /* Todo write more tests */
        final List<AchillesAnalysis> list = dao.findAll();
        assert(obj1.getAnalysisId() == 1);
        assert(list.get(0).getAnalysisId() == obj1.getAnalysisId());
    }

    private AchillesAnalysis createAnalysis(int aid, String name) {
        return new AchillesAnalysis()
                .analysisId((long)aid)
                .analysisName(name)
                .stratum1Name("stratum1 name")
                .stratum2Name("stratum 2 name")
                .stratum3Name("stratum 3 name")
                .stratum4Name("stratum 4 name")
                .stratum5Name("stratum 5 name")
                .chartType("column")
                .dataType("count");
    }

}
