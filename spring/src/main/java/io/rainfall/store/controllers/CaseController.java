package io.rainfall.store.controllers;

import io.rainfall.store.dataset.CaseDataset;
import io.rainfall.store.dataset.CaseRecord;
import io.rainfall.store.values.Case;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;

import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.SEE_OTHER;

@Controller
@SuppressWarnings("unused")
class CaseController extends DatasetController<CaseRecord, CaseDataset> {

  @Autowired
  CaseController(CaseDataset dataset) {
    super(dataset, "Case", "/cases");
  }

  @GetMapping("/")
  public ModelAndView root(ModelMap model) {
    return new ModelAndView("redirect:/cases", model);
  }

  @GetMapping("/cases")
  public ModelAndView getCases(ModelMap model) {
    model.addAttribute("cases", dataset().getRecords());
    model.addAttribute("caseForm", new CaseForm());
    return new ModelAndView("cases", model);
  }

  @GetMapping({ "/cases/{id}" })
  public ModelAndView getCase(ModelMap model, @PathVariable long id) {
    return get(model, id);
  }

  @PostMapping(path = "/cases")
  public ResponseEntity<?> postCase(@Valid @ModelAttribute("caseForm") CaseForm form,
                                    BindingResult result) {
    if (result.hasErrors()) {
      String errors = listErrors(result);
      throw new IllegalArgumentException(errors);
    }
    Case testCase = form.build();
    long id = dataset()
        .save(testCase)
        .getId();
    return post(id, SEE_OTHER);
  }

  private String listErrors(BindingResult result) {
    return result.getAllErrors()
        .stream()
        .map(ObjectError::getDefaultMessage)
        .collect(joining("; ", "", "."));
  }

  @GetMapping("/compare")
  public ModelAndView getCompareForm(ModelMap model) {
    model.addAttribute("cases", dataset().getRecords());
    return new ModelAndView("compare-form", model);
  }
}
