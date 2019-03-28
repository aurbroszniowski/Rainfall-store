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

package io.rainfall.store.core;

import java.util.Objects;

public class ClientJob {

  private final int clientNumber;
  private final String host;
  private final String symbolicName;
  private final String details;

  private ClientJob(Builder builder) {
    clientNumber = builder.clientNumber;
    host = builder.host;
    symbolicName = builder.symbolicName;
    details = builder.details;
  }

  public static Builder builder() {
    return new Builder();
  }

  public int getClientNumber() {
    return clientNumber;
  }

  public String getHost() {
    return host;
  }

  public String getSymbolicName() {
    return symbolicName;
  }

  public String getDetails() {
    return details;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClientJob clientJob = (ClientJob)o;
    return clientNumber == clientJob.clientNumber &&
           Objects.equals(host, clientJob.host) &&
           Objects.equals(details, clientJob.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clientNumber, host, details);
  }

  @Override
  public String toString() {
    return "ClientJob{" +
           "clientNumber=" + clientNumber +
           ", host='" + host + '\'' +
           ", details='" + details + '\'' +
           '}';
  }

  public static class Builder implements io.rainfall.store.core.Builder<ClientJob> {

    private int clientNumber;
    private String host;
    private String symbolicName;
    private String details;

    public Builder clientNumber(int number) {
      this.clientNumber = number;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder symbolicName(String symbolicName) {
      this.symbolicName = symbolicName;
      return this;
    }

    public Builder details(String details) {
      this.details = details;
      return this;
    }

    @Override
    public ClientJob build() {
      return new ClientJob(this);
    }
  }
}
