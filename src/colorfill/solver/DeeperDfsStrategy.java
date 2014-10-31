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

import java.util.List;
import java.util.Set;

import colorfill.model.Board;
import colorfill.model.ColorArea;

/**
 * this strategy results in an incomplete search.
 * it chooses the colors in two steps:
 * <p>
 * 1) colors that can be completely flooded in the next step.
 * (these are always optimal moves!?)
 * <p>
 * 2 a) if 1) gives no result then the colors that are situated at depth + 1
 * or lower. (hence the name "deeper")
 */
public class DeeperDfsStrategy implements DfsStrategy {

    private final int maxDepth;

    public DeeperDfsStrategy(final Board board, final int startPos) {
        this.maxDepth = board.getDepth(startPos);
    }

    /* (non-Javadoc)
     * @see colorfill.solver.DfsStrategy#selectColors(int, java.lang.Integer, java.util.List, java.util.Set, colorfill.solver.ColorAreaGroup, colorfill.solver.ColorAreaGroup)
     */
    @Override
    public List<Integer> selectColors(final int depth,
            final Integer thisColor,
            final List<Integer> solution,
            final Set<ColorArea> flooded,
            final ColorAreaGroup notFlooded,
            final ColorAreaGroup neighbors) {
        List<Integer> result = neighbors.getColorsCompleted(notFlooded);
        if (result.size() > 0) {
            return result;
        }

        // slow. score(100)=2082  score(1000)=20815   262 seconds
        for (int i = Math.min(depth + 1, this.maxDepth);  i > 0;  --i) {
            result = neighbors.getColorsDepth(i);
            if (result.size() > 0) {
                return result;
            }
        }
        return result;

        // very slow! score(100)=2071  score(1000)=???
//        if (depth < this.maxDepth) {
//            for (int i = depth + 1;  i > 0;  --i) {
//                result = neighbors.getColorsDepth(i);
//                if (result.size() > 0) {
//                    return result;
//                }
//            }
//            return result;
//        } else {
//            return neighbors.getColorsNotEmpty();
//        }

    }
}
