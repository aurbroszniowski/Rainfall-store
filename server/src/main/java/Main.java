import io.rainfall.store.record.Store;
import io.rainfall.store.record.tc.RainfallStore;
import io.rainfall.store.service.spark.StoreController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.terracottatech.store.StoreException;
import com.terracottatech.store.configuration.DatasetConfiguration;
import com.terracottatech.store.configuration.MemoryUnit;
import com.terracottatech.store.manager.DatasetManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

import static com.terracottatech.store.manager.DatasetManager.embedded;
import static com.terracottatech.store.manager.EmbeddedDatasetManagerBuilder.FileMode.REOPEN_OR_NEW;
import static com.terracottatech.store.manager.EmbeddedDatasetManagerBuilder.PersistenceMode.HYBRID;

public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);


  public static void main(String[] args) throws StoreException, IOException {
    Properties props = findProperties();

    long offheap = Long.valueOf(props.getProperty("offheap", "50"));
    LOGGER.info("Dataset offheap size={}", offheap);

    String diskLocation = props.getProperty("disk", "./performance");
    LOGGER.info("Dataset disk resource={}", diskLocation);

    int port = Integer.valueOf(props.getProperty("port", "4567"));
    LOGGER.info("Web service port={}", port);

    String urlPath = props.getProperty("path", "performance");
    LOGGER.info("Web service path={}", urlPath);

    DatasetManager datasetManager = embedded()
        .offheap("offheap", offheap, MemoryUnit.MB)
        .disk("disk", Paths.get(diskLocation), HYBRID, REOPEN_OR_NEW)
        .build();
    DatasetConfiguration config = datasetManager.datasetConfiguration()
        .offheap("offheap")
        .disk("disk")
        .build();
    Store store = new RainfallStore(datasetManager, config)
        .indexParents();
    new StoreController(store, urlPath, port)
        .awaitInitialization();
  }

  private static Properties findProperties() throws IOException {
    Properties props = new Properties();
    String propsFileName = System.getProperty("propsFile");
    if (propsFileName != null) {
      readProperties(props, propsFileName);
    }
    props.putAll(System.getProperties());
    return props;
  }

  private static void readProperties(Properties props, String propsFileName) throws IOException {
    try (InputStream is = Main.class.getResourceAsStream(propsFileName)) {
      LOGGER.info("Reading properties from {}.", propsFileName);
      props.load(is);
    }
  }
}
