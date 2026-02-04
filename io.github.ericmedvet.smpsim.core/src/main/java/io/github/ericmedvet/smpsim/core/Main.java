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

import io.github.ericmedvet.smpsim.core.Environment.Configuration;
import io.github.ericmedvet.smpsim.core.Environment.State;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class Main {

  static void main() throws IOException {
    Configuration configuration = new Configuration(
        Location.of(128, 128),
        true,
        3,
        3,
        64
    );
    int maxK = 20000;
    int nOfInstructions = 16;
    State initialState = configuration.initialState(boolean[]::new);
    StateDrawer drawer = new StateDrawer(StateDrawer.Configuration.DEFAULT);
    for (int seed = 0; seed < 100; seed++) {
      RandomGenerator rg = new Random(seed);
      List<Instruction> instructions = generateInstructions(configuration, 8, rg);
      Environment environment = new Environment(configuration, instructions);
      State state = initialState;
      List<Integer> popSizes = new ArrayList<>();
      while (!state.individuals().isEmpty() && state.k() <= maxK) {
        popSizes.add(state.individuals().size());
        state = environment.step(state);
      }
      System.out.printf("seed = %3d\t", seed);
      System.out.printf("final k = %8d\t", state.k());
      System.out.printf("max pop size = %d\t", popSizes.stream().max(Integer::compare).orElse(0));
      System.out.printf(
          "avg pop size = %.1f%n",
          popSizes.stream().mapToDouble(s -> s).average().orElse(0)
      );
      if (state.k() > 100) {
        drawer.save(new File("../%03d.svg".formatted(seed)), state);
      }
    }
  }

  private static List<Instruction> generateInstructions(
      Configuration configuration,
      int n,
      RandomGenerator rg
  ) {
    return IntStream.range(0, n)
        .mapToObj(
            i -> Instruction.random(
                configuration.maxLocation().coords().length,
                configuration.instructionInputSize(),
                configuration.instructionOutputSize(),
                rg
            )
        )
        .toList();
  }

}