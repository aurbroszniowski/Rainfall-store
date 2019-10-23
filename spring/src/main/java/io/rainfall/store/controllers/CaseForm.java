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

import io.rainfall.store.values.Case;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

class CaseForm {

  @Size(min = 1, max = 255, message = "Name must be 1-255 characters long")
  @NotNull
  @Setter
  @Getter
  @NonNull
  private String name;

  @Size(max = 1024, message = "Description must be up to 1024 characters long")
  @Setter
  @Getter
  @NonNull
  private String description;

  Case build() {
    return Case.builder()
        .name(name)
        .description(description)
        .build();
  }
}
