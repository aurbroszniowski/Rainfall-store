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

package io.rainfall.store.service.spark;

import io.rainfall.store.record.Store;
import io.rainfall.store.service.Result;
import io.rainfall.store.service.StoreService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Service;
import spark.template.mustache.MustacheTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static org.eclipse.jetty.http.MimeTypes.Type;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_HTML;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_PLAIN;


public class StoreController implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreController.class);

  private final Service service;
  private final Gson gson = new Gson();
  private final MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

  public StoreController(Store store, String path, int port) {
    StoreService perfService = new StoreService(store);
    service = Service.ignite();
    service.port(port);
    service.path(path, () -> {
      service.staticFiles.location("/js");

      Stream.of("", "/", "/cases/")
          .forEach(url -> redirect(path, url));
      show("/cases", homePage(perfService));

      service.post("/cases", (q, s) -> createTestCase(perfService, q, s));
      show("/cases/:name",
          perfService::getTestCase, "case.mustache", ":name");
      show("/cases/:name/runs",
          perfService::getRuns, "runs.mustache", ":name");
      get("/cases/:name/runs/json", perfService::getRuns, APPLICATION_JSON, ":name");

      post("/runs/:parentId", perfService::addRun);
      service.post("/runs/:id/status",
          (q, s) -> create(q, s, perfService::setStatus, ":id"));
      service.post("/runs/:id/baseline",
          (q, s) -> create(q, s, perfService::setBaseline, ":id"));
      show("/runs/:id",
          perfService::getRun, "run.mustache", ":id");
      show("/runs/:id/jobs",
          perfService::getClientJobs, "jobs.mustache", ":id");
      show("/runs/:id/stats",
          perfService::getStats, "stats.mustache", ":id");
      show("/runs/:id/stats/:host",
          (q, s) -> getStatsForRunAndHost(perfService, q, "stats.mustache"));
      get("/runs/:id/operations",
          perfService::getOperationsForRun);
      get("/runs/:ids/common-operations",
          perfService::getCommonOperationsForRuns,
          APPLICATION_JSON, ":ids");
      service.get("/runs/:id/aggregate/:operation",
          (q, s) -> getAggregateHdrData(perfService, q, s));
      service.get("/runs/:id/regression/:threshold",
          (q, s) -> checkRegression(perfService, q, s));

      post("/jobs/:parentId", perfService::addClientJob);
      show("/jobs/:id",
          perfService::getClientJob, "job.mustache", ":id");
      get("/jobs/:id/outputs", perfService::getOutputs);

      post("/outputs/:parentId", perfService::addOutput);
      get("/outputs/:id", perfService::getOutputData, TEXT_PLAIN, ":id");
      get("/outputs/:id/io.rainfall.store.service.spark", perfService::getHdrData);

      post("/stats/:parentId", perfService::addStatsLog);
      get("/stats/:id", perfService::getStatsLog, TEXT_PLAIN, ":id");

      show("/compare", (q, s) -> new ModelAndView(perfService.listTestCases(),
          "compare-form.mustache"));
      show("/compare/:ids", perfService::compareRuns,
          "compare-report.mustache", ":ids");
      service.get("/compare/:ids/:operation",
          (q, s) -> getComparativeHdrData(perfService, q, s));
    });
  }

  @SuppressWarnings("SameParameterValue")
  private ModelAndView getStatsForRunAndHost(
      StoreService perfService, Request request, String template) {
    LOGGER.info("GET: {}.", request.pathInfo());
    String sid = request.params().get(":id");
    String host = request.params().get(":host");
    Result result = perfService.getStats(sid, host);
    return modelAndView(result, template);
  }

  private void redirect(String path, String url) {
    service.get(url, (request, response) -> {
      response.redirect("/" + path + "/cases", HTTP_SEE_OTHER);
      return null;
    });
  }

  private BiFunction<Request, Response, ModelAndView> homePage(StoreService perfService) {
    return (request, response) -> testCasesModelAndView(perfService);
  }

  private void show(String path,
                    BiFunction<Request, Response, ModelAndView> modelAndViewGenerator) {
    service.get(path, (q, s) -> {
      LOGGER.info("GET: {}.", q.pathInfo());
      return templateEngine.render(modelAndViewGenerator.apply(q, s));
    });
  }

  private ModelAndView testCasesModelAndView(StoreService perfService) {
    return new ModelAndView(perfService.listTestCases(),
        "cases.mustache");
  }

  private void show(String path, Function<String, Result> getter,
                    String template, String key) {
    show(path, (q, s) -> modelAndView(q, getter, template, key));
  }

  private ModelAndView modelAndView(Request request, Function<String, Result> getter,
                                    String template, String key) {
    String uniqueName = request.params()
        .get(key);
    Result result = getter.apply(uniqueName);
    return modelAndView(result, template);
  }

  private ModelAndView modelAndView(Result result, String template) {
    String viewName = result.getCode() == HTTP_OK
        ? template
        : "msg.mustache";
    return new ModelAndView(result.getContent(), viewName);
  }

  private Object createTestCase(StoreService perfService, Request request, Response response) {
    String name = request.queryParams("name");
    String description = request.queryParams("description");
    Result result = perfService.addTestCase(name, description);
    switch (result.getCode()) {
      case HTTP_CREATED:
        return redirectToTestCaseCreated(result, response);
      default:
        return resultWithEditedResponse(result, response);
    }
  }

  private Object redirectToTestCaseCreated(Result result, Response response) {
    response.status(HTTP_SEE_OTHER);
    response.header("Location", "cases/" + result.getContent());
    return "";
  }

  private Object create(Request request, Response response,
                        Function<String, Result> op) {
    String body = request.body();
    Result result = op.apply(body);
    return resultWithEditedResponse(result, response);
  }

  private Object resultWithEditedResponse(Result result, Response response) {
    return resultWithEditedResponse(result, response, TEXT_HTML);
  }

  private Object resultWithEditedResponse(Result result, Response response, Type defaulType) {
    return resultWithEditedResponse(result, response, result.getCode(), defaulType);
  }

  private Object resultWithEditedResponse(Result result, Response response, int code, Type defaulType) {
    response.status(code);
    Type type = contentType(code, defaulType);
    response.raw().setContentType(type.asString());
    return format(type, result.getContent());
  }

  private void post(String path, BiFunction<String, String, Result> creator) {
    service.post(path, (q, s) -> create(q, s, creator));
  }

  private Object create(Request request, Response response,
                        BiFunction<String, String, Result> op) {
    return create(request, response, op, ":parentID");
  }

  private Object create(Request request, Response response,
                        BiFunction<String, String, Result> op, String key) {
    LOGGER.info("POST: {}.", request.pathInfo());
    String id = request.params(key);
    return create(request, response, body -> op.apply(id, body));
  }

  private void get(String path, Function<String, Result> getter) {
    get(path, getter, APPLICATION_JSON, ":id");
  }

  private void get(String path, Function<String, Result> getter, Type defaultType, String key) {
    service.get(path, (q, s) -> {
      LOGGER.info("GET: {}.", q.pathInfo());
      String sid = q.params().get(key);
      Result result = getter.apply(sid);
      return resultWithEditedResponse(result, s, defaultType);
    });
  }

  private Object getAggregateHdrData(
      StoreService perfService, Request request, Response response) {
    String sid = request.params().get(":id");
    String operation = request.params().get(":operation");
    Result result = perfService.getAggregateHdrData(sid, operation);
    return resultWithEditedResponse(result, response, APPLICATION_JSON);
  }

  private Object checkRegression(
      StoreService perfService, Request request, Response response) {
    String sid = request.params().get(":id");
    String threshold = request.params().get(":threshold");
    Result result = perfService.checkRegression(sid, threshold);
    return resultWithEditedResponse(result, response, APPLICATION_JSON);
  }

  private Object getComparativeHdrData(
      StoreService perfService, Request request, Response response) {
    String sids = request.params().get(":ids");
    String operation = request.params().get(":operation");
    Result result = perfService.getComparativeHdrData(sids, operation);
    return resultWithEditedResponse(result, response, APPLICATION_JSON);
  }

  private Type contentType(int code, Type defaultType) {
    switch (code) {
      case HTTP_OK:
        return defaultType;
      case HTTP_CREATED:
      default:
        return TEXT_HTML;
    }
  }

  private Object format(Type type, Object content) {
    switch (type) {
      case APPLICATION_JSON:
        return gson.toJson(content);
      default:
        return content;
    }
  }

  public StoreController awaitInitialization() {
    service.awaitInitialization();
    return this;
  }

  @Override
  public void close() {
    service.stop();
  }
}
