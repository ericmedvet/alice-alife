/*-
 * ========================LICENSE_START=================================
 * smpsim-core
 * %%
 * Copyright (C) 2018 - 2026 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.ericmedvet.smpsim.core;

import java.util.Arrays;
import java.util.Objects;

public final class Individual {

  private final long kOfBirth;
  private Location location;
  private boolean[] memory;

  public Individual(
      Location location,
      long kOfBirth,
      boolean[] memory
  ) {
    this.location = location;
    this.kOfBirth = kOfBirth;
    this.memory = memory;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (Individual) obj;
    return Objects.equals(this.location, that.location) && this.kOfBirth == that.kOfBirth && Arrays.equals(
        this.memory,
        that.memory
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, kOfBirth, Arrays.hashCode(memory));
  }

  public long kOfBirth() {
    return kOfBirth;
  }

  public Location location() {
    return location;
  }

  public boolean[] memory() {
    return memory;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public void setMemory(boolean[] memory) {
    this.memory = memory;
  }

  @Override
  public String toString() {
    return "{%s;%d;%s}".formatted(
        location,
        kOfBirth,
        Utils.bitStringToString(memory)
    );
  }

}
