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

package io.rainfall.store.service;

import org.eclipse.jetty.http.MimeTypes;

import java.util.Objects;

public class Result {

  private final int code;
  private final MimeTypes.Type contentType;
  private final Object content;

  public Result(int code, MimeTypes.Type contentType, Object content) {
    this.code = code;
    this.contentType = contentType;
    this.content = content;
  }

  public int getCode() {
    return code;
  }

  public MimeTypes.Type getContentType() {
    return contentType;
  }

  public Object getContent() {
    return content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Result result = (Result)o;
    return code == result.code &&
           Objects.equals(contentType, result.contentType) &&
           Objects.equals(content, result.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, contentType, content);
  }

  @Override
  public String toString() {
    return "Result{" +
           "code=" + code +
           ", contentType='" + contentType + '\'' +
           ", content=" + content +
           '}';
  }
}
