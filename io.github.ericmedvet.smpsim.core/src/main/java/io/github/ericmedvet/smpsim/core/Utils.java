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

public class Utils {

  private Utils() {
  }

  public static int bitStringToInt(boolean[] bitString) {
    int n = 0;
    for (int i = bitString.length - 1; i >= 0; i--) {
      n = (n << 1) | (bitString[i] ? 1 : 0);
    }
    return n;
  }

  public static boolean[] intToSizedBitString(int n, int size) {
    boolean[] bits = new boolean[size];
    for (int i = size - 1; i >= 0; i--) {
      bits[i] = (n & (1 << i)) != 0;
    }
    return bits;
  }

  public static boolean[] intToBitString(int n, int max) {
    return intToSizedBitString(n, ceilLog2(max));
  }

  public static int ceilLog2(int n) {
    int k = n - 1;
    int log = 0;
    if ((k & 0xffff0000) != 0) {
      k >>>= 16;
      log = 16;
    }
    if (k >= 256) {
      k >>>= 8;
      log += 8;
    }
    if (k >= 16) {
      k >>>= 4;
      log += 4;
    }
    if (k >= 4) {
      k >>>= 2;
      log += 2;
    }
    int out = log + (k >>> 1);
    return out + 1;
  }

  public static boolean isPowerOfTwo(int n) {
    return n == Math.powExact(2, ceilLog2(n));
  }

  public static String bitStringToString(boolean[] bitString) {
    StringBuilder sb = new StringBuilder();
    for (int i = bitString.length - 1; i >= 0; i = i - 1) {
      sb.append(bitString[i] ? "1" : "0");
    }
    return sb.toString();
  }

  public static boolean[] stringToBitString(String string) {
    boolean[] bits = new boolean[string.length()];
    for (int i = bits.length - 1; i >= 0; i = i - 1) {
      bits[i] = switch (string.substring((string.length() - 1 - i), (string.length() - i))) {
        case "0" -> false;
        case "1" -> true;
        default -> throw new IllegalArgumentException(
            "Wrong char in string %s at %d: %s".formatted(string, i, string.substring(i, i))
        );
      };
    }
    return bits;
  }

  public static boolean[] concat(boolean[]... bitStrings) {
    int size = Arrays.stream(bitStrings).mapToInt(s -> s.length).sum();
    boolean[] out = new boolean[size];
    int i = 0;
    for (boolean[] string : bitStrings) {
      System.arraycopy(string, 0, out, i, string.length);
      i = i + string.length;
    }
    return out;
  }

  public static boolean[] subBitString(boolean[] bitString, int index, int length) {
    boolean[] out = new boolean[length];
    System.arraycopy(bitString, index, out, 0, length);
    return out;
  }
}
