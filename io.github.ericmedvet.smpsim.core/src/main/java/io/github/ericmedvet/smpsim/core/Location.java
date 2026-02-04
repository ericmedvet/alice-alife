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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record Location(int[] coords) {

  public static Location origin(int nOfDimensions) {
    return new Location(new int[nOfDimensions]);
  }

  public static Location of(int... coords) {
    return new Location(coords);
  }

  public Set<Location> boundedLocations() {
    Set<Location> locations = new LinkedHashSet<>();
    Location last = Location.origin(coords.length);
    while (last.isWithin(this)) {
      locations.add(last);
      last = last.next(this);
    }
    return locations;
  }

  public boolean isWithin(Location bound) {
    if (coords.length != bound.coords.length) {
      throw new IllegalArgumentException(
          "Wrong coords size: %d != %d".formatted(coords.length, bound.coords.length)
      );
    }
    for (int i = 0; i < coords.length; i = i + 1) {
      if (coords[i] < 0 || coords[i] >= bound.coords[i]) {
        return false;
      }
    }
    return true;
  }

  private Location next(Location bound) {
    int[] newCoords = Arrays.copyOf(coords, coords.length);
    newCoords[0] = newCoords[0] + 1;
    for (int i = 1; i < newCoords.length; i = i + 1) {
      if (newCoords[i - 1] >= bound.coords[i - 1]) {
        newCoords[i - 1] = 0;
        newCoords[i] = newCoords[i] + 1;
      } else {
        break;
      }
    }
    return new Location(newCoords);
  }

  @Override
  public String toString() {
    return "(%s)".formatted(
        Arrays.stream(coords)
            .mapToObj(Integer::toString)
            .collect(
                Collectors.joining(";")
            )
    );
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Location(int[] otherCoords))) {
      return false;
    }
    return Objects.deepEquals(coords(), otherCoords);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(coords());
  }
}
