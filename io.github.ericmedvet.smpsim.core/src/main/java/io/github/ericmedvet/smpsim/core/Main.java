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

import io.github.ericmedvet.jnb.datastructure.Accumulator;
import io.github.ericmedvet.jviz.core.drawer.Video;
import io.github.ericmedvet.jviz.core.util.VideoUtils.EncoderFacility;
import io.github.ericmedvet.smpsim.core.Environment.Configuration;
import io.github.ericmedvet.smpsim.core.Environment.State;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class Main {

  static void main() throws IOException {
    RandomGenerator rg = RandomGenerator.getDefault();
    Configuration configuration = new Configuration(
        Location.of(64, 64),
        false,
        4,
        3,
        100
    );
    List<Instruction> instructions = IntStream.range(0, 8)
        .mapToObj(
            i -> Instruction.random(
                configuration.maxLocation().coords().length,
                configuration.dataSize() + configuration.individualMemorySize(),
                rg
            )
        )
        .toList();
    State initialState = configuration.initialState(s -> Utils.randomBitString(s, rg));
    Environment environment = new Environment(configuration, instructions);
    StateDrawer drawer = new StateDrawer(StateDrawer.Configuration.DEFAULT);
    Accumulator<State, Video> accumulator = drawer.videoAccumulator(
        30,
        EncoderFacility.DEFAULT
    );
    int maxK = 1000;
    State state = initialState;
    List<Integer> popSizes = new ArrayList<>();
    while (!state.individuals().isEmpty() && state.k() <= maxK) {
      accumulator.listen(state);
      popSizes.add(state.individuals().size());
      state = environment.step(state);
    }
    System.out.printf("final k = %d%n", state.k());
    System.out.printf("max pop size = %d%n", popSizes.stream().max(Integer::compare).orElse(0));
    System.out.printf("avg pop size = %.1f%n", popSizes.stream().mapToDouble(s -> s).average().orElse(0));
    Files.write(Path.of("../sim.mp4"), accumulator.get().data(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    drawer.show(state);
  }

}