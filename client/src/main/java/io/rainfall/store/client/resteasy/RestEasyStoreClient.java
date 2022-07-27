/*
 * Copyright (c) 2014-2019 Aurélien Broszniowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rainfall.store.client.resteasy;

import io.rainfall.store.client.StoreClient;
import io.rainfall.store.core.ChangeReport;
import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.MetricsLog;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.record.MetricsRec;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.zalando.jersey.gson.internal.GsonJsonProvider;

import java.util.List;
import java.util.function.Function;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;

import static javax.ws.rs.client.Entity.json;
import static org.slf4j.LoggerFactory.getLogger;

public class RestEasyStoreClient implements StoreClient {

  private static final org.slf4j.Logger LOGGER = getLogger(RestEasyStoreClient.class);

  private static final ClientRequestFilter LOGGING_FILTER = request ->
      LOGGER.info("{} {} {}", new Object[] { request.getMethod(), request.getUri(), request.getEntity() });

  private static final String PARENT_PARAM = "parentId";

  private final String contextUrl;

  public RestEasyStoreClient(String contextUrl) {
    this.contextUrl = contextUrl;
  }

  @Override
  public void addTestCase(String uniqueName, TestCase testCase) {
    throw new UnsupportedOperationException("TestCases must be added in web interface!");
  }

  @Override
  public long addRun(String caseId, TestRun run) {
    return add("runs", caseId, run);
  }

  @Override
  public long addClientJob(long runId, ClientJob job) {
    return add("jobs", runId, job);
  }

  @Override
  public long addOutput(long jobId, OperationOutput output) {
    return add("outputs", jobId, output);
  }

  @Override
  public long addStatsLog(long runId, StatsLog log) {
    return add("stats", runId, log);
  }

  @Override
  public long addMetricsLog(MetricsLog metricsLog) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean setStatus(long runId, TestRun.Status status) {
    String statusName = status.name();
    String path = "runs" + "/{" + PARENT_PARAM + "}" + "/status";
    return post(path, runId, statusName, Boolean::valueOf);
  }

  @Override
  public List<MetricsRec> listMetricsRec() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetricsRec getMetricsRec(Long id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteMetricsRec(Long id) {
    throw new UnsupportedOperationException();
  }

  private long add(String path, Object parentId, Object value) {
    return post(path + "/{" + PARENT_PARAM + "}",
        parentId, value, Long::valueOf);
  }

  private <V> V post(String path, Object id, Object value, Function<String, V> parser) {
    Client client = new ResteasyClientBuilderImpl()
        .register(GsonJsonProvider.class)
        .build();
    try {
      client.register(LOGGING_FILTER);
      WebTarget target = client.target(contextUrl)
          .path(path);
      String result = target.resolveTemplate(PARENT_PARAM, id)
          .request()
          .post(json(value))
          .readEntity(String.class);
      return parser.apply(result);
    } finally {
      client.close();
    }
  }

  @Override
  public ChangeReport checkRegression(long runId, double threshold) {
    Client client = new ResteasyClientBuilderImpl()
        .register(GsonJsonProvider.class)
        .build();

    try {
      client.register(LOGGING_FILTER);
      WebTarget target = client.target(contextUrl)
          .path("runs/{runId}/regression/{threshold}");
      return target.resolveTemplate("runId", runId)
          .resolveTemplate("threshold", threshold)
          .request()
          .get()
          .readEntity(ChangeReport.class);
    } finally {
      client.close();
    }
  }
}
