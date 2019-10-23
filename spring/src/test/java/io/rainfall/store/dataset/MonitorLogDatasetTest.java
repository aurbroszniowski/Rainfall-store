package io.rainfall.store.dataset;

import io.rainfall.store.RainfallStoreApp;
import io.rainfall.store.data.Payload;
import io.rainfall.store.values.Case;
import io.rainfall.store.values.MonitorLog;
import io.rainfall.store.values.Run;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static io.rainfall.store.values.Run.Status.INCOMPLETE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@DataJpaTest
@ComponentScan("io.rainfall.store.dataset")
@ContextConfiguration(classes = RainfallStoreApp.class)
@RunWith(SpringRunner.class)
@SuppressWarnings({ "ConstantConditions", "UnusedDeclaration" })
public class MonitorLogDatasetTest {

  @Autowired
  private CaseDataset caseDataset;

  @Autowired
  private RunDataset runDataset;

  @Autowired
  private MonitorLogDataset logDataset;

  private final Run run = Run.builder()
      .status(INCOMPLETE)
      .version("1.1.1.1")
      .baseline(true)
      .className("my.Class")
      .build();

  @Test
  public void testSaveWithNonExistentParent() {
    MonitorLog value = MonitorLog.builder()
        .build();
    try {
      logDataset.save(0L, value);
      fail();
    } catch (Throwable e) {
      assertThat(e, instanceOf(IllegalArgumentException.class));
    }
  }

  @Test
  public void testSave() throws IOException {
    long parentId = saveParent();

    Payload payload = Utils.readBytes("150.hlog");
    MonitorLog log = MonitorLog.builder()
        .host("localhost")
        .type("vmstat")
        .payload(payload)
        .build();

    MonitorLogRecord childRecord = logDataset.save(parentId, log);
    MonitorLog saved = childRecord.getValue();
    assertThat(saved, is(log));
    assertThat(saved.getPayload(), is(payload));
    assertThat(childRecord.getParent().getValue(), is(run));

    Optional<MonitorLogRecord> found = logDataset.getRecord(childRecord.getId());

    RunRecord parentRecord = found
        .map(ChildRecord::getParent)
        .get();
    assertThat(parentRecord.getValue(), is(run));
    assertThat(parentRecord.getMonitorLogs(), contains(childRecord));

    Payload payloadFound = found.map(MonitorLogRecord::getPayloadRecord)
        .map(Record::getValue)
        .orElse(null);
    assertThat(payloadFound, is(payload));
  }

  @Test
  public void testFindByParentId() {
    long parentId = saveParent();
    MonitorLog log = MonitorLog.builder()
        .host("localhost")
        .type("vmstat")
        .payload(Payload.raw("xxx"))
        .build();
    MonitorLogRecord childRecord = logDataset.save(parentId, log);
    List<MonitorLogRecord> children = logDataset.findByParentId(parentId);
    assertThat(children, contains(childRecord));
  }

  private long saveParent() {
    Case testCase = Case.builder().build();
    long caseId = caseDataset.save(testCase).getId();
    return runDataset.save(caseId, run).getId();
  }
}
