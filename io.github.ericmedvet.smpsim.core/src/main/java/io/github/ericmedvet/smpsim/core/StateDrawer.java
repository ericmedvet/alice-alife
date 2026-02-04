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

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import io.github.ericmedvet.jviz.core.plot.image.ColorRange;
import io.github.ericmedvet.smpsim.core.Environment.Configuration;
import io.github.ericmedvet.smpsim.core.Environment.State;
import io.github.ericmedvet.smpsim.core.StateDrawer.Configuration.InfoType;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class StateDrawer implements Drawer<Environment.State> {

  private final Configuration c;

  public StateDrawer(Configuration configuration) {
    this.c = configuration;
  }

  @Override
  public void draw(Graphics2D g, State state) {
    if (state.configuration().maxLocation().coords().length != 2) {
      throw new UnsupportedOperationException(
          "Drawer works only for 2D states: %d found".formatted(
              state.configuration().maxLocation().coords().length
          )
      );
    }
    int envW = state.configuration().maxLocation().coords()[0];
    int envH = state.configuration().maxLocation().coords()[1];
    double scaleX = (double) g.getClipBounds().width / (double) envW;
    double scaleY = (double) g.getClipBounds().height / (double) envH;
    ColorRange colorRange = new ColorRange(c.cellMinColor, c.cellMaxColor);
    DoubleRange dataRange = new DoubleRange(0, Math.powExact(2, state.configuration().dataSize()));
    // draw cells
    state.data().forEach((l, bs) -> {
      g.setColor(colorRange.interpolate(dataRange.normalize(Utils.bitStringToInt(bs))));
      g.fill(
          new Rectangle2D.Double(
              l.coords()[0] * scaleX,
              l.coords()[1] * scaleY,
              scaleX,
              scaleY
          )
      );
    });
    // draw individuals
    g.setColor(c.individualColor);
    state.individuals()
        .stream()
        .collect(Collectors.groupingBy(Individual::location))
        .forEach((l, individuals) -> {
          g.draw(
              new Ellipse2D.Double(
                  l.coords()[0] * scaleX,
                  l.coords()[1] * scaleY,
                  scaleX,
                  scaleY
              )
          );
          double r = 1 - (double) individuals.size() / state.configuration().maxNOfIndividuals();
          g.fill(
              new Ellipse2D.Double(
                  (l.coords()[0] + r / 2) * scaleX,
                  (l.coords()[1] + r / 2) * scaleY,
                  scaleX * (1 - r),
                  scaleY * (1 - r)
              )
          );
        });
    // draw info
    if (!c.infoTypes.isEmpty()) {
      g.setColor(c.infoColor);
      int h = g.getFontMetrics().getHeight();
      StringBuilder sb = new StringBuilder();
      if (c.infoTypes.contains(InfoType.K)) {
        sb.append("k=%05d ".formatted(state.k()));
      }
      if (c.infoTypes.contains(InfoType.N_OF_INDIVIDUALS)) {
        sb.append("n=%3d ".formatted(state.individuals().size()));
      }
      g.drawString(sb.toString(), 1, 1 + h);
    }
  }

  @Override
  public ImageInfo imageInfo(State state) {
    if (state.configuration().maxLocation().coords().length != 2) {
      throw new UnsupportedOperationException(
          "Drawer works only for 2D states: %d found".formatted(
              state.configuration().maxLocation().coords().length
          )
      );
    }
    return new ImageInfo(
        c.cellSize * state.configuration().maxLocation().coords()[0],
        c.cellSize * state.configuration().maxLocation().coords()[1]
    );
  }

  public record Configuration(
      int cellSize,
      Color cellMinColor,
      Color cellMaxColor,
      Color individualColor,
      Color infoColor,
      Set<InfoType> infoTypes
  ) {

    public static Configuration DEFAULT = new Configuration(
        5,
        Color.BLACK,
        Color.BLUE,
        Color.YELLOW,
        Color.WHITE,
        EnumSet.of(InfoType.K, InfoType.N_OF_INDIVIDUALS)
    );

    public enum InfoType { K, N_OF_INDIVIDUALS }
  }
}
