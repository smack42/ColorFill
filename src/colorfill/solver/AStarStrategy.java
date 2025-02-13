/*  ColorFill game and solver
    Copyright (C) 2014, 2015, 2021 Michael Henke

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
 * a strategy for the AStar (A*) solver.
 */
public interface AStarStrategy extends Strategy {

    /**
     * compute the cost estimation using a heuristic
     */
    public int estimateCost(final AStarNode node, int nonCompletedColors);
}
