package io.rainfall.store.dataset;

import io.rainfall.store.RainfallStoreApp;
import io.rainfall.store.values.Case;
import io.rainfall.store.values.Run;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

import static io.rainfall.store.values.Run.Status.COMPLETE;
import static io.rainfall.store.values.Run.Status.INCOMPLETE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@DataJpaTest
@ComponentScan("io.rainfall.store.dataset")
@ContextConfiguration(classes = RainfallStoreApp.class)
@RunWith(SpringRunner.class)
@SuppressWarnings({ "ConstantConditions", "UnusedDeclaration" })
public class RunDatasetTest {

  @Autowired
  private CaseDataset caseDataset;

  @Autowired
  private RunDataset runDataset;


  private final Case testCase = Case.builder()
      .name("Test1")
      .build();

  private final Run run = Run.builder()
      .status(INCOMPLETE)
      .version("1.1.1.1")
      .baseline(false)
      .className("my.Class")
      .build();

  @Test
  public void testSaveWithNonExistentParent() {
    Run value = Run.builder()
        .build();
    try {
      runDataset.save(0L, value);
      fail();
    } catch (Throwable e) {
      assertThat(e, instanceOf(IllegalArgumentException.class));
    }
  }

  @Test
  public void testSave() {
    long parentId = saveParent();
    RunRecord childRecord = runDataset.save(parentId, run);
    assertThat(childRecord.getValue(), is(run));
    assertThat(childRecord.getParent().getValue(), is(testCase));

    CaseRecord parentRecord = runDataset.getRecord(childRecord.getId())
        .map(RunRecord::getParent)
        .get();
    assertThat(parentRecord.getValue(), is(testCase));
    assertThat(parentRecord.getRuns(), contains(childRecord));
  }

  @Test
  public void testFindByParentId() {
    long parentId = saveParent();
    RunRecord childRecord = runDataset.save(parentId, run);
    List<RunRecord> children = runDataset.findByParentId(parentId);
    assertThat(children, contains(childRecord));
  }

  @Test
  public void testSetStatus() {
    long parentId = saveParent();
    long id = runDataset.save(parentId, run)
        .getId();
    runDataset.setStatus(id, COMPLETE);
    assertThat(
        runDataset.getRecord(id)
            .map(Record::getValue)
            .map(Run::getStatus)
            .get(),
        is(COMPLETE)
    );
  }

  @Test
  public void testSetBaseline() {
    long parentId = saveParent();
    long id = runDataset.save(parentId, run)
        .getId();
    runDataset.setBaseline(id, true);
    assertThat(
        runDataset.getRecord(id)
            .map(Record::getValue)
            .map(Run::isBaseline)
            .get(),
        is(true)
    );
  }

  @Test
  public void testGetLastBaseline() {
    long parentId = saveParent();
    long runId1 = runDataset.save(parentId, run)
        .getId();

    Optional<Long> notFound = runDataset.getLastBaselineID(parentId);
    assertFalse(notFound.isPresent());

    runDataset.setBaseline(runId1, true);

    Optional<Long> first = runDataset.getLastBaselineID(parentId);
    assertTrue(first.isPresent());
    assertThat(first.get(), is(runId1));
    assertThat(runDataset.getLastBaselineID(-1), is(notFound));

    long runId2 = runDataset.save(parentId, run)
        .getId();
    assertEquals(runDataset.getLastBaselineID(parentId), first);

    runDataset.setBaseline(runId2, true);
    Optional<Long> second = runDataset.getLastBaselineID(parentId);
    assertTrue(second.isPresent());
    assertThat(second.get(), is(runId2));

    runDataset.setBaseline(runId2, false);
    assertEquals(runDataset.getLastBaselineID(parentId), first);

    runDataset.setBaseline(runId1, false);
    assertEquals(runDataset.getLastBaselineID(parentId), notFound);
  }

  private long saveParent() {
    return caseDataset.save(testCase).getId();
  }
}
