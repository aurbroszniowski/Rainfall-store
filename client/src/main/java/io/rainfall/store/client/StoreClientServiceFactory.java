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

import io.rainfall.store.client.resteasy.RestEasyStoreClient;
import io.rainfall.store.data.CompressionService;

import static io.rainfall.store.data.CompressionFormat.LZ4;
import static io.rainfall.store.data.CompressionServiceFactory.compressionService;

@SuppressWarnings("WeakerAccess")
public class StoreClientServiceFactory {

  public static final StoreClientService NOOP = new StoreClientService() {
  };

  /**
   * Create a StoreClientService.
   * This should be called on the test machine.
   *
   * @param URL URL of the store REST service, or null.
   * @return if url is null, a noop service which doesn't store anything.
   * Otherwise, a StoreClientService connecting to the given URL.
   */
  public static StoreClientService defaultService(String URL) {
    return URL == null ? NOOP : forURL(URL);
  }

  private static StoreClientService forURL(String URL) {
    StoreClient client = new RestEasyStoreClient(URL);
    CompressionService compressionService = compressionService(LZ4);
    return new DefaultStoreClientService(client, compressionService);
  }
}
