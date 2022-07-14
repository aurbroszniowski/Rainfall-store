package io.rainfall.store.record.tc;

import io.rainfall.store.core.MetricsLog;
import io.rainfall.store.record.MetricsRec;

import com.terracottatech.store.Cell;
import com.terracottatech.store.Dataset;
import com.terracottatech.store.DatasetReader;
import com.terracottatech.store.Record;
import com.terracottatech.store.definition.LongCellDefinition;
import com.terracottatech.store.definition.StringCellDefinition;
import com.terracottatech.store.stream.RecordStream;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static com.terracottatech.store.definition.CellDefinition.defineLong;
import static com.terracottatech.store.definition.CellDefinition.defineString;

/**
 * @author Aurelien Broszniowski
 */

public class MetricsDataset {

  private static final LongCellDefinition RUN_ID = defineLong("id");
  private static final StringCellDefinition CLOUD_TYPE = defineString("cloud_type");

  private final AtomicLong idGenerator;
  private static final Comparator<Record<Long>> ID_COMPARATOR = Record.<Long>keyFunction().asComparator();
  private final Dataset<Long> dataset;

  public MetricsDataset(Dataset<Long> dataset) {
    this.dataset = dataset;
    this.idGenerator = initIdGenerator();
  }

  private AtomicLong initIdGenerator() {
    Long lastId = lastId();
    return new AtomicLong(lastId);
  }

  private Long lastId() {
    DatasetReader<Long> reader = dataset.reader();
    return reader.records()
        .max(ID_COMPARATOR)
        .map(Record::getKey)
        .orElse(0L);
  }

  private Long key() {
    return idGenerator.incrementAndGet();
  }

  public Long add(MetricsLog metricsLog) {
    long key = key();
    final String label = metricsLog.getLabel();
    final String cloudType = metricsLog.getCloudType();
    final String metrics = metricsLog.getMetrics();
    System.out.println("Label = " + label + " - cloudType = " + cloudType + " - metrics = " + metrics);
    dataset.writerReader().add(key, Cell.cell("label", label),
          Cell.cell("cloudType", cloudType),
          Cell.cell("metrics", metrics));
      return key;
    }

  public List<MetricsRec> list() {
    List<MetricsRec> metricsRecList = new ArrayList<>();
    final RecordStream<Long> records = dataset.reader().records();
    records.forEach(r -> {
      final Long key = r.getKey();
      final Optional<?> label = r.get("label");
      String l = null;
      if (label.isPresent()) {
        l = (String)label.get();
      }
      final Optional<?> cloudType = r.get("cloudType");
      String c = null;
      if (cloudType.isPresent()) {
        c = (String) cloudType.get();
      }
      final Optional<?> metrics = r.get("metrics");
      String m = null;
      if (metrics.isPresent()) {
        m = (String)metrics.get();
      }
      metricsRecList.add(new MetricsRec(key, new MetricsLog(l, c, m)));
    });
    return metricsRecList;
  }

  public MetricsRec get(Long id) {
    final Optional<Record<Long>> cells = dataset.reader().get(id);
    final MetricsRec metricsRec = new MetricsRec(id, new MetricsLog(
        (String)cells.get().get("label").get(), (String)cells.get().get("cloudType").get(),
        (String)cells.get().get("metrics").get()));
    return metricsRec;
  }

  public boolean delete(Long id) {
    return dataset.writerReader().delete(id);
  }
}