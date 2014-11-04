/*  ColorFill game and solver
    Copyright (C) 2014 Michael Henke

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package colorfill.solver;

import java.util.Set;

import it.unimi.dsi.fastutil.bytes.ByteList;

import colorfill.model.ColorArea;

/**
 * a strategy for the depth-first search (DFS) solver.
 */
public interface DfsStrategy extends Strategy {

    /**
     * select one or more colors (from neighbors) for the next step of the depth-first search.
     * 
     * @param depth current DFS depth
     * @param thisColor color used in this step
     * @param solution the solution so far
     * @param flooded the flooded area of the board
     * @param notFlooded the area of the board not flooded yet
     * @param neighbors the neighbor areas of the flooded area
     * @return the colors to be used for the next step
     */
    public ByteList selectColors(int depth,
            byte thisColor,
            byte[] solution,
            Set<ColorArea> flooded,
            ColorAreaGroup notFlooded,
            ColorAreaGroup neighbors);
}
