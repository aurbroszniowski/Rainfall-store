package io.rainfall.store.dataset;

import io.rainfall.store.RainfallStoreApp;
import io.rainfall.store.values.Case;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@DataJpaTest
@ComponentScan("io.rainfall.store.dataset")
@ContextConfiguration(classes = RainfallStoreApp.class)
@RunWith(SpringRunner.class)
@SuppressWarnings({ "ConstantConditions", "UnusedDeclaration" })
public class CaseDatasetTest {

  @Autowired
  private CaseDataset dataset;

  @Test
  public void testSave() {
    Case value = Case.builder()
        .name("Test1")
        .description("Some test")
        .build();
    CaseRecord record = dataset.save(value);
    assertThat(record.getValue(), is(value));
    assertThat(
        dataset.getRecord(record.getId()).get(),
        is(record)
    );
  }

  @Test
  public void testDefaults() {
    Case value = Case.builder()
        .build();
    long id = dataset.save(value).getId();
    Case storedValue = dataset.getRecord(id)
        .map(CaseRecord::getValue)
        .get();
    assertThat(storedValue.getName(), is("No name"));
    assertThat(storedValue.getDescription(), is("No description"));
  }

  @Test
  public void testDuplicateNames() {
    Case value = Case.builder()
        .name("Name")
        .build();
    dataset.save(value);
    try {
      dataset.save(value);
      fail();
    } catch (RuntimeException e) {
      //expected
    }
  }

  @Test
  public void testSetDescription() {
    Case value = Case.builder()
        .name("Test1")
        .build();
    long id = dataset.save(value).getId();
    String updated = "Updated";
    dataset.setDescription(id, updated);
    assertThat(
        dataset.getRecord(id)
            .map(CaseRecord::getValue)
            .map(Case::getDescription)
            .get(),
        is(updated)
    );
  }
}
