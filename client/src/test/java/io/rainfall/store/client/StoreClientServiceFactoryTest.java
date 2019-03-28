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

package io.rainfall.store.client;


import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class StoreClientServiceFactoryTest {

  @Test
  public void testDefault() {
    StoreClientService service = StoreClientServiceFactory.defaultService("http://localhost:8080");
    assertThat(service, instanceOf(DefaultStoreClientService.class));
  }

  @Test
  public void testNoop() {
    StoreClientService service = StoreClientServiceFactory.defaultService(null);
    assertThat(service, is(StoreClientServiceFactory.NOOP));
  }
}
