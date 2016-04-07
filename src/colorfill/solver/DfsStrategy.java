/*  ColorFill game and solver
    Copyright (C) 2014, 2015 Michael Henke

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

/**
 * a strategy for the depth-first search (DFS) solver.
 */
public interface DfsStrategy extends Strategy {

    /**
     * select one or more colors (from neighbors) for the next step of the depth-first search.
     * 
     * @param depth current DFS depth
     * @param flooded the flooded area of the board
     * @param notFlooded the area of the board not flooded yet
     * @param neighbors the neighbor areas of the flooded area
     * @return the colors to be used for the next step
     */
    public byte[] selectColors(int depth,
            ColorAreaSet flooded,
            ColorAreaGroup notFlooded,
            ColorAreaGroup neighbors);

    /**
     * get some info from the strategy, like memory usage after solver has finished.
     * @return some info, may be null or empty if the particular solver has nothing to say.
     */
    public String getInfo();
}
