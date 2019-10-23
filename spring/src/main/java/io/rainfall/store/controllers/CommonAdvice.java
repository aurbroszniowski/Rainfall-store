package io.rainfall.store.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
@SuppressWarnings("unused")
public class CommonAdvice {

  private Log logger = LogFactory.getLog(CommonAdvice.class);

  @Value("${server.servlet.context-path:/performance}")
  private String contextPath;

  @ModelAttribute
  public void addContextPath(Model model) {
    model.addAttribute("context-path", contextPath);
  }

  @ExceptionHandler(value = Exception.class)
  public ModelAndView handleException(HttpServletRequest request, Exception ex) {
    logger.error("Request " + request.getRequestURL()
                 + " Threw an Exception", ex);
    ModelAndView modelAndView = new ModelAndView();
    modelAndView.addObject("exception", ex);
    modelAndView.addObject("url", request.getRequestURL());
    modelAndView.addObject("method", request.getMethod());
    modelAndView.setViewName("error");
    return modelAndView;
  }
}
