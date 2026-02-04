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

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

public record Instruction(
    Continuation continuation,
    List<Movement> movements,
    List<boolean[]> semantics
) implements UnaryOperator<boolean[]> {

  public Instruction {
    // check num of cases
    if (!Utils.isPowerOfTwo(semantics.size())) {
      throw new IllegalArgumentException(
          "Wrong number of cases: %d is not a power of 2".formatted(semantics.size())
      );
    }
    // check case size consistency
    List<Integer> caseSizes = semantics.stream()
        .mapToInt(c -> c.length)
        .distinct()
        .boxed()
        .toList();
    if (caseSizes.size() != 1) {
      throw new IllegalArgumentException(
          "Non uniform size of cases: %s".formatted(
              caseSizes.stream()
                  .map(i -> Integer.toString(i))
                  .collect(
                      Collectors.joining("; ")
                  )
          )
      );
    }
    // check case i/o size consistency
    if (caseSizes.getFirst() != Utils.ceilLog2(semantics.size())) {
      throw new IllegalArgumentException(
          "Non-matching case input and output szie: %d != %d".formatted(
              Utils.ceilLog2(semantics.size()),
              caseSizes.getFirst()
          )
      );
    }
  }

  public Instruction(int nOfDimensions, int ioSize, boolean[] bitString) {
    // check size
    int expectedSize = size(nOfDimensions, ioSize);
    if (bitString.length != expectedSize) {
      throw new IllegalArgumentException(
          "Wrong input size: %d found, %d+%d*%d+%d*%d=%d expected".formatted(
              bitString.length,
              Utils.ceilLog2(Continuation.values().length),
              nOfDimensions,
              Utils.ceilLog2(Movement.values().length),
              Math.powExact(2, ioSize),
              ioSize,
              expectedSize
          )
      );
    }
    int j = 0;
    Continuation lContinuation = valid(
        Continuation.class,
        Utils.bitStringToInt(
            Utils.subBitString(bitString, j, Utils.ceilLog2(Continuation.values().length))
        )
    );
    j = j + Utils.ceilLog2(Continuation.values().length);
    List<Movement> lMovements = new ArrayList<>();
    for (int i = 0; i < nOfDimensions; i = i + 1) {
      lMovements.add(
          valid(
              Movement.class,
              Utils.bitStringToInt(
                  Utils.subBitString(bitString, j, Utils.ceilLog2(Movement.values().length))
              )
          )
      );
      j = j + Utils.ceilLog2(Movement.values().length);
    }
    List<boolean[]> lSemantics = new ArrayList<>();
    for (int i = 0; i < Math.powExact(2, ioSize); i = i + 1) {
      lSemantics.add(Utils.subBitString(bitString, j, ioSize));
      j = j + ioSize;
    }
    this(lContinuation, lMovements.stream().toList(), lSemantics.stream().toList());
  }

  public static Instruction random(int nOfDimensions, int ioSize, RandomGenerator rg) {
    return new Instruction(
        nOfDimensions,
        ioSize,
        Utils.randomBitString(size(nOfDimensions, ioSize), rg)
    );
  }

  public static int size(int nOfDimensions, int ioSize) {
    int expectedSize = 0;
    expectedSize = expectedSize + Utils.ceilLog2(Continuation.values().length);
    expectedSize = expectedSize + nOfDimensions * Utils.ceilLog2(Movement.values().length);
    expectedSize = expectedSize + Math.powExact(2, ioSize) * ioSize;
    return expectedSize;
  }

  private static <E extends Enum<E>> E valid(Class<E> enumClass, int index) {
    if (index >= enumClass.getEnumConstants().length) {
      return enumClass.getEnumConstants()[0];
    }
    return enumClass.getEnumConstants()[index];
  }

  @Override
  public boolean[] apply(boolean[] input) {
    if (input.length != Utils.ceilLog2(semantics.size())) {
      throw new IllegalArgumentException(
          "Wrong input size: %d found, %d expected".formatted(
              input.length,
              Utils.ceilLog2(semantics.size())
          )
      );
    }
    return semantics.get(Utils.bitStringToInt(input));
  }

  public int ioSize() {
    return semantics.getFirst().length;
  }

  public boolean[] toBitString() {
    boolean[][] chunks = new boolean[1 + movements.size() + semantics.size()][];
    chunks[0] = Utils.intToBitString(continuation.ordinal(), Continuation.values().length);
    for (int i = 0; i < movements.size(); i = i + 1) {
      chunks[1 + i] = Utils.intToBitString(movements.get(i).ordinal(), Movement.values().length);
    }
    int j = 1 + movements.size();
    for (int i = 0; i < semantics.size(); i = i + 1) {
      chunks[j + i] = semantics.get(i);
    }
    return Utils.concat(chunks);
  }

  @Override
  public String toString() {
    return "<%s;%s;%s>".formatted(
        switch (continuation) {
          case NONE -> "_";
          case DEATH -> "-";
          case REPLICATION -> "+";
        },
        movements.stream().map(m -> switch (m) {
          case STAY -> "o";
          case DECREASE -> "-";
          case INCREASE -> "+";
        }).collect(Collectors.joining()),
        semantics.stream()
            .map(Utils::bitStringToString)
            .collect(Collectors.joining(","))
    );
  }

  public enum Continuation { NONE, DEATH, REPLICATION }

  public enum Movement { STAY, DECREASE, INCREASE }

}