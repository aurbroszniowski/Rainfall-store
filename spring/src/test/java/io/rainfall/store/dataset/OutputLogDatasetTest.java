package io.rainfall.store.dataset;

import io.rainfall.store.RainfallStoreApp;
import io.rainfall.store.Utils;
import io.rainfall.store.data.Payload;
import io.rainfall.store.values.Case;
import io.rainfall.store.values.Job;
import io.rainfall.store.values.OutputLog;
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
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@DataJpaTest
@ComponentScan("io.rainfall.store.dataset")
@ContextConfiguration(classes = RainfallStoreApp.class)
@RunWith(SpringRunner.class)
@SuppressWarnings({ "ConstantConditions", "UnusedDeclaration" })
public class OutputLogDatasetTest {

  @Autowired
  private CaseDataset caseDataset;

  @Autowired
  private RunDataset runDataset;

  @Autowired
  private JobDataset jobDataset;

  @Autowired
  private OutputLogDataset outputLogDataset;

  private final Job job = Job.builder()
      .clientNumber(1)
      .host("localhost")
      .symbolicName("localhost-1")
      .details("details")
      .build();

  private long runId;

  @Test
  public void testSaveWithNonExistentParent() {
    OutputLog value = OutputLog.builder()
        .build();
    try {
      outputLogDataset.save(0L, value);
      fail();
    } catch (Throwable e) {
      assertThat(e, instanceOf(IllegalArgumentException.class));
    }
  }

  @Test
  public void testSave() throws IOException {
    long parentId = saveParent();

    Payload payload = Utils.readBytes("150.hlog");
    OutputLog outputLog = OutputLog.builder()
        .operation("MISS")
        .format("hlog")
        .payload(payload)
        .build();

    OutputLogRecord childRecord = outputLogDataset.save(parentId, outputLog);
    OutputLog saved = childRecord.getValue();
    assertThat(saved, is(outputLog));
    assertThat(saved.getPayload(), is(payload));
    assertThat(childRecord.getParent().getValue(), is(job));

    Optional<OutputLogRecord> found = outputLogDataset.getRecord(childRecord.getId());

    JobRecord parentRecord = found.map(ChildRecord::getParent)
        .get();
    assertThat(parentRecord.getValue(), is(job));
    assertThat(parentRecord.getOutputLogs(), contains(childRecord));

    Payload payloadFound = found.map(OutputLogRecord::getPayloadRecord)
        .map(Record::getValue)
        .orElse(null);
    assertThat(payloadFound, is(payload));
  }

  @Test
  public void testFindByParentId() {
    long parentId = saveParent();
    OutputLog outputLog = forOp("MISS");
    OutputLogRecord childRecord = outputLogDataset.save(parentId, outputLog);
    List<OutputLogRecord> children = outputLogDataset.findByParentId(parentId);
    assertThat(children, contains(childRecord));
  }

  @Test
  public void testFindOperationsByRunId() {
    long parentId = saveParent();
    Stream.of("GET", "GET", "MISS")
        .map(this::forOp)
        .forEach(op -> outputLogDataset.save(parentId, op));
    List<String> records = outputLogDataset
        .findOperationsByRunId(runId);
    assertThat(records, containsInAnyOrder("MISS", "GET"));
  }

  @Test
  public void testFindOutputLogsByRunIdAndOperation() {
    long parentId = saveParent();
    OutputLog outputLog = forOp("MISS");
    OutputLogRecord record = outputLogDataset.save(parentId, outputLog);
    List<Payload> records = outputLogDataset
        .findOutputLogsByRunIdAndOperation(runId, "MISS");
    assertThat(records, contains(record.getPayloadRecord().getValue()));
  }

  @Test
  public void testGetCommonOperationsForRuns() {
    Case testCase = Case.builder().build();
    long caseId = caseDataset.save(testCase).getId();

    Run run = Run.builder().build();

    long runId1 = runDataset.save(caseId, run).getId();
    long jobId1 = jobDataset.save(runId1, job).getId();
    outputLogDataset.save(jobId1, forOp("GET"));
    outputLogDataset.save(jobId1, forOp("MISS"));

    long runId2 = runDataset.save(caseId, run).getId();
    long jobId2 = jobDataset.save(runId2, job).getId();
    outputLogDataset.save(jobId2, forOp("GET"));

    Set<String> commonOps = outputLogDataset
        .findCommonOperationsForRuns(runId1, runId2);
    assertThat(commonOps, contains("GET"));
  }

  private long saveParent() {
    Case testCase = Case.builder().build();
    long caseId = caseDataset.save(testCase).getId();

    Run run = Run.builder().build();
    runId = runDataset.save(caseId, run).getId();

    return jobDataset.save(runId, job).getId();
  }

  private OutputLog forOp(String op) {
    return OutputLog.builder()
        .operation(op)
        .format("hlog")
        .payload(Payload.raw("xxx"))
        .build();
  }
}
