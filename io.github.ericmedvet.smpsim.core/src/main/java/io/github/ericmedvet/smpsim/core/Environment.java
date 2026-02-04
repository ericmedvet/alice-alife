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

import io.github.ericmedvet.smpsim.core.Instruction.Continuation;
import io.github.ericmedvet.smpsim.core.Instruction.Movement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Environment {

  private final Configuration configuration;
  private final List<Instruction> instructions;

  public Environment(Configuration configuration, List<Instruction> instructions) {
    this.configuration = configuration;
    this.instructions = instructions;
    // check instructions ioSize consistency
    for (Instruction instruction : instructions) {
      if (instruction.inputSize() != configuration.instructionInputSize() || instruction.outputSize() != configuration
          .instructionOutputSize()) {
        throw new IllegalArgumentException(
            "Wrong IO size for instruction %s: %d->%d != %d->%d".formatted(
                instruction,
                instruction.inputSize(),
                instruction.outputSize(),
                configuration.instructionInputSize(),
                configuration.instructionOutputSize()
            )
        );
      }
    }
  }

  private static int coord(int i, int bound, boolean toroidal) {
    if (i < 0) {
      return toroidal ? (bound - 1) : 0;
    }
    if (i >= bound) {
      return toroidal ? 0 : (bound - 1);
    }
    return i;
  }

  private static Location update(
      Location location,
      List<Movement> movements,
      Location maxLocation,
      boolean toroidal
  ) {
    if (movements.size() != location.coords().length) {
      throw new IllegalArgumentException(
          "Wrong size of movements: %d != %d".formatted(
              movements.size(),
              location.coords().length
          )
      );
    }
    int[] coords = Arrays.copyOf(location.coords(), location.coords().length);
    for (int i = 0; i < location.coords().length; i = i + 1) {
      coords[i] = switch (movements.get(i)) {
        case DECREASE -> coord(coords[i] - 1, maxLocation.coords()[i], toroidal);
        case INCREASE -> coord(coords[i] + 1, maxLocation.coords()[i], toroidal);
        case STAY -> coord(coords[i], maxLocation.coords()[i], toroidal);
      };
    }
    return new Location(coords);
  }

  public State step(State state) {
    record AgedData(long kOfBirth, boolean[] bitString) {

    }
    List<Integer> toRemoveIndividuals = new ArrayList<>();
    List<Individual> toAddIndividuals = new ArrayList<>();
    Map<Location, AgedData> changedData = new HashMap<>();
    // iterate over individuals
    Map<Location, boolean[]> occupancy = state.individuals.stream()
        .collect(
            Collectors.groupingBy(
                Individual::location,
                Collectors.collectingAndThen(
                    Collectors.counting(),
                    n -> Utils.intToBitString(n.intValue(), configuration.maxNOfIndividuals)
                )
            )
        );
    for (int i = 0; i < state.individuals.size(); i = i + 1) {
      Individual individual = state.individuals.get(i);
      boolean[] lData = state.data.get(individual.location());
      int instructionIndex = Utils.bitStringToInt(
          Utils.subBitString(lData, 0, Utils.ceilLog2(instructions.size()))
      );
      Instruction instruction = instructions.get(instructionIndex % instructions.size());
      // check death or duplication
      if (instruction.continuation().equals(Continuation.DEATH)) {
        toRemoveIndividuals.add(i);
        continue;
      }
      if (instruction.continuation().equals(Continuation.REPLICATION) && (state.individuals.size() - toRemoveIndividuals
          .size() + toAddIndividuals.size()) < configuration.maxNOfIndividuals) {
        toAddIndividuals.add(
            new Individual(
                individual.location(),
                state.k,
                new boolean[configuration.individualMemorySize]
            )
        );
      }
      // process data
      boolean[] input = Utils.concat(
          lData,
          occupancy.get(individual.location()),
          individual.memory()
      );
      boolean[] output = instruction.apply(input);
      boolean[] newData = Utils.subBitString(output, 0, lData.length);
      boolean[] newMemory = Utils.subBitString(
          output,
          newData.length,
          configuration.individualMemorySize
      );
      changedData.merge(
          individual.location(),
          new AgedData(individual.kOfBirth(), newData),
          (oldAgedData, newAgedData) -> (oldAgedData.kOfBirth <= newAgedData.kOfBirth) ? oldAgedData : newAgedData
      );
      individual.setMemory(newMemory);
      individual.setLocation(
          update(
              individual.location(),
              instruction.movements(),
              configuration.maxLocation,
              configuration.isToroidal
          )
      );
    }
    // apply changes
    List<Integer> toKeepIndividualIndexes = IntStream.range(0, state.individuals.size())
        .boxed()
        .filter(i -> !toRemoveIndividuals.contains(i))
        .toList();
    Map<Location, boolean[]> newData = new HashMap<>(state.data);
    changedData.forEach((l, agedData) -> newData.put(l, agedData.bitString));
    return new State(
        state.k + 1,
        newData,
        Stream.concat(
            toKeepIndividualIndexes.stream().map(state.individuals::get),
            toAddIndividuals.stream()
        ).toList(),
        configuration
    );
  }

  public record Configuration(
      Location maxLocation,
      boolean isToroidal,
      int dataSize,
      int individualMemorySize,
      int maxNOfIndividuals
  ) {

    public int instructionInputSize() {
      return dataSize + Utils.ceilLog2(maxNOfIndividuals) + individualMemorySize;
    }

    public int instructionOutputSize() {
      return dataSize + individualMemorySize;
    }

    private void checkState(State state) {
      // check location consistency
      state.data()
          .keySet()
          .forEach(
              l -> {
                if (!l.isWithin(maxLocation)) {
                  throw new IllegalArgumentException(
                      "Wrong coords size: %s not withing %s".formatted(l, maxLocation)
                  );
                }
              }
          );
      // check location recall
      List<Location> missingLocations = maxLocation.boundedLocations()
          .stream()
          .filter(
              key -> !state.data.containsKey(key)
          )
          .toList();
      if (!missingLocations.isEmpty()) {
        throw new IllegalArgumentException(
            "Missing locations in initial state: %s".formatted(missingLocations)
        );
      }
      // check individuals locations
      state.individuals().forEach(i -> {
        if (!i.location().isWithin(maxLocation)) {
          throw new IllegalArgumentException(
              "Wrong individual location: %s not withing %s".formatted(
                  i.location(),
                  maxLocation
              )
          );
        }
      });
      // check individuals memory size
      state.individuals.forEach(i -> {
        if (i.memory().length != individualMemorySize) {
          throw new IllegalArgumentException(
              "Wrong individual memory size: %d != %d".formatted(
                  i.memory().length,
                  individualMemorySize
              )
          );
        }
      });
    }

    public State initialState(IntFunction<boolean[]> dataSupplier) {
      State state = new State(
          0,
          maxLocation().boundedLocations()
              .stream()
              .collect(
                  Collectors.toMap(
                      l -> l,
                      _ -> dataSupplier.apply(dataSize)
                  )
              ),
          List.of(
              new Individual(
                  new Location(
                      Arrays.stream(maxLocation.coords())
                          .map(c -> c / 2)
                          .toArray()
                  ),
                  0,
                  new boolean[individualMemorySize()]
              )
          ),
          this
      );
      checkState(state);
      return state;
    }

  }

  public record State(
      long k,
      Map<Location, boolean[]> data,
      List<Individual> individuals,
      Configuration configuration
  ) {

    public int nOfFilledLocations() {
      return (int) individuals.stream().map(Individual::location).distinct().count();
    }
  }
}