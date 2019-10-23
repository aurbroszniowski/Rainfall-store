package io.rainfall.store.controllers;

import io.rainfall.store.dataset.CaseDataset;
import io.rainfall.store.dataset.CaseRecord;
import io.rainfall.store.dataset.Record;
import io.rainfall.store.dataset.RunDataset;
import io.rainfall.store.dataset.RunRecord;
import io.rainfall.store.values.Run;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@SuppressWarnings("unused")
public class RunController extends ChildController<Run, RunRecord, CaseRecord, RunDataset> {

  @Autowired
  CaseDataset caseDataset;

  @Autowired
  RunController(RunDataset dataset) {
    super(dataset, "Run", "/runs");
  }

  @PostMapping("/runs/{caseName}")
  public ResponseEntity<?> postRun(@PathVariable String caseName, @RequestBody Run run) {
    Optional<CaseRecord> caseRecord = caseDataset.findByName(caseName);
    Long caseId = caseRecord.map(Record::getId)
        .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseName));
    return super.post(caseId, run);
  }

  @GetMapping({ "/cases/{parentId}/runs" })
  public ModelAndView getRunsByCaseID(ModelMap model, @PathVariable long parentId) {
    return getByParentId(model, parentId);
  }

  @GetMapping({ "/cases/{parentId}/runs/json" })
  @ResponseBody
  public List<RunRecord> listRunsByCaseID(@PathVariable long parentId) {
    return dataset().findByParentId(parentId);
  }

  @GetMapping({ "/runs/{id}" })
  public ModelAndView getRun(ModelMap model, @PathVariable long id) {
    return get(model, id);
  }

  @PostMapping(path = "/runs/{id}/baseline", consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<?> setBaseline(@PathVariable long id,
                                       @RequestBody boolean baseline) {
    dataset().setBaseline(id, baseline);
    return ResponseEntity.ok(baseline);
  }

  @PostMapping(path = "/runs/{id}/status")
  public ResponseEntity<?> setStatus(@PathVariable long id,
                                     @RequestBody String statusName) {
    Run.Status status = Run.Status.valueOf(statusName);
    dataset().setStatus(id, status);
    return ResponseEntity.ok(status);
  }

  @GetMapping({ "/compare/{sids}" })
  public ModelAndView getCompareReport(ModelMap model, @PathVariable String sids) {
    long[] ids = parseIds(sids);
    List<RunRecord> runs = dataset().findByIds(ids);
    model.addAttribute("runs", runs);
    return new ModelAndView("compare-report", model);
  }
}
