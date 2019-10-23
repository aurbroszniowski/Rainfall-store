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

package io.rainfall.store.controllers;


import io.rainfall.store.RainfallStoreApp;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ContextConfiguration(classes = RainfallStoreApp.class)
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
abstract class ControllerIT {

  static final String DEFAULT_TEXT_HTML = "text/html;charset=UTF-8";

  @Autowired
  private WebApplicationContext context;

  MockMvc mvc;

  @Before
  public void setup() {
    mvc = webAppContextSetup(context).build();
  }

  @SuppressWarnings("unchecked")
  Matcher<String> containsAll(String... patterns) {
    Matcher[] matchers = Stream.of(patterns)
        .map(Matchers::containsString)
        .toArray(Matcher[]::new);
    return Matchers.allOf(matchers);
  }
}
