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
import java.util.stream.Collectors;

public record Instruction(
    Order order,
    Continuation continuation,
    List<Movement> movements,
    List<boolean[]> semantics
) implements UnaryOperator<boolean[]> {

  public Instruction {
    // check num of cases
    if (Utils.isPowerOfTwo(semantics.size())) {
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

  public enum Order { MOVE_AND_PROCESS, PROCESS_AND_MOVE }

  public enum Continuation { NONE, DEATH, REPLICATION }

  public enum Movement { STAY, DECREASE, INCREASE }

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

  public boolean[] toBitString() {
    boolean[][] chunks = new boolean[1 + 1 + movements.size() + semantics.size()][];
    chunks[0] = Utils.intToBitString(order.ordinal(), Order.values().length);
    chunks[1] = Utils.intToBitString(continuation.ordinal(), Continuation.values().length);
    for (int i = 0; i < movements.size(); i = i + 1) {
      chunks[2 + i] = Utils.intToBitString(movements.get(i).ordinal(), Movement.values().length);
    }
    int j = 2 + movements.size();
    for (int i = 0; i < semantics.size(); i = i + 1) {
      chunks[j + i] = semantics.get(i);
    }
    return Utils.concat(chunks);
  }

  public Instruction(int nOfDimensions, int ioSize, boolean[] bits) {
    // check size
    int expectedSize = 0;
    expectedSize = expectedSize + Utils.ceilLog2(Order.values().length);
    expectedSize = expectedSize + Utils.ceilLog2(Continuation.values().length);
    expectedSize = expectedSize + nOfDimensions * Utils.ceilLog2(Movement.values().length);
    expectedSize = expectedSize + Math.powExact(2, ioSize) * ioSize;
    if (bits.length != expectedSize) {
      throw new IllegalArgumentException(
          "Wrong input size: %d found, %d+%d*%d*%d+%d*%d=%d expected".formatted(
              bits.length,
              Utils.ceilLog2(Order.values().length),
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
    Order lOrder = valid(
        Order.class,
        Utils.bitStringToInt(Utils.subBitString(bits, j, Utils.ceilLog2(Order.values().length)))
    );
    j = j + Utils.ceilLog2(Order.values().length);
    Continuation lContinuation = valid(
        Continuation.class,
        Utils.bitStringToInt(
            Utils.subBitString(bits, j, Utils.ceilLog2(Continuation.values().length))
        )
    );
    j = j + Utils.ceilLog2(Continuation.values().length);
    List<Movement> lMovements = new ArrayList<>();
    for (int i = 0; i < nOfDimensions; i = i + 1) {
      lMovements.add(
          valid(
              Movement.class,
              Utils.bitStringToInt(
                  Utils.subBitString(bits, j, Utils.ceilLog2(Movement.values().length))
              )
          )
      );
      j = j + Utils.ceilLog2(Movement.values().length);
    }
    List<boolean[]> lSemantics = new ArrayList<>();
    for (int i = 0; i < Math.powExact(2, ioSize); i = i + 1) {
      lSemantics.add(Utils.subBitString(bits, j, ioSize));
      j = j + ioSize;
    }
    this(lOrder, lContinuation, lMovements.stream().toList(), lSemantics.stream().toList());
  }

  private static <E extends Enum<E>> E valid(Class<E> enumClass, int index) {
    if (index >= enumClass.getEnumConstants().length) {
      return enumClass.getEnumConstants()[0];
    }
    return enumClass.getEnumConstants()[index];
  }

  @Override
  public String toString() {
    return "<%s;%s;%s;%s>".formatted(
        switch (order) {
          case MOVE_AND_PROCESS -> "m";
          case PROCESS_AND_MOVE -> "p";
        },
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
}
