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

package colorfill.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import colorfill.solver.Solution;

public class GameProgress {

    private final String name;
    private final boolean isModifiable;

    private final Board board;

    private int numSteps;
    private final List<Integer> stepColor = new ArrayList<Integer>();
    private final List<HashSet<ColorArea>> stepFlooded = new ArrayList<HashSet<ColorArea>>();
    private final List<HashSet<ColorArea>> stepFloodNext = new ArrayList<HashSet<ColorArea>>();

    /**
     * construct a "user progress" object.
     * @param board
     * @param startPos
     */
    protected GameProgress(final Board board, final int startPos) {
        this.name = "User";
        this.isModifiable = true;
        this.board = board;
        this.initSteps(startPos);
    }

    /**
     * construct a "solver progress" object. (finished solution, can only redo/undo)
     * @param board
     * @param startPos
     */
    protected GameProgress(final Board board, final int startPos, final Solution solution) {
        this.name = solution.getSolverName();
        this.isModifiable = false;
        this.board = board;
        this.initSteps(startPos);
        for (final byte color : solution.getSteps()) {
            this.addStep(color, true);
        }
        this.numSteps = 0;
    }

    /**
     * construct a "user progress" object, from a saved state (step + strStepColors).
     * @param board
     * @param startPos
     * @param step
     * @param strStepColors
     */
    protected GameProgress(final Board board, final int startPos, final int step, final String strStepColors) {
        this(board, startPos);
        if ((null != strStepColors) && (0 != strStepColors.length())) {
            for (final char c : strStepColors.toCharArray()) {
                this.addStep(Integer.parseInt(String.valueOf(c)) - 1, true);
            }
            this.numSteps = step;
        }
    }

    private void initSteps(final int startPos) {
        this.numSteps = 0;
        this.stepColor.clear();
        this.stepColor.add(Integer.valueOf(this.board.getColor(startPos)));
        this.stepFlooded.clear();
        this.stepFlooded.add(new HashSet<ColorArea>(Collections.singleton(this.board.getColorArea4Cell(startPos))));
        this.stepFloodNext.clear();
        this.stepFloodNext.add(new HashSet<ColorArea>(this.board.getColorArea4Cell(startPos).getNeighbors()));
    }

    /**
     * try to append a new step to the progress of the game.
     * this may fail if the specified color is the current
     * flood color (no color change) or if no unflooded cells
     * are left on the board (puzzle finished).
     * 
     * @param color
     * @return true if the step was actually added
     */
    public boolean addStep(final int color) {
        return this.addStep(color, false);
    }

    private boolean addStep(final int color, final boolean init) {
        if ((false == this.isModifiable) && (false == init)) {
            throw new IllegalStateException("addStep error: this GameProgress \"" + this.name + "\" is not modifiable");
        }
        final Integer col = Integer.valueOf(color);
        // check if same color as before or nothing to be flooded
        if (this.stepColor.get(this.numSteps).equals(col)
                || this.stepFloodNext.get(this.numSteps).isEmpty()) {
            return false;
        }
        // determine new flooded area
        final Set<ColorArea> newFlood = new HashSet<ColorArea>();
        for (final ColorArea ca : this.stepFloodNext.get(this.numSteps)) {
            if (ca.getColor() == color) {
                newFlood.add(ca);
            }
        }
        if (newFlood.isEmpty()) {
            return false; // this color is not a flood neighbor
        }
        // current lists are too long (because of undo) - remove the future moves
        if (this.stepColor.size() > this.numSteps + 1) {
            this.stepColor.subList(this.numSteps + 1, this.stepColor.size()).clear();
            this.stepFlooded.subList(this.numSteps + 1, this.stepFlooded.size()).clear();
            this.stepFloodNext.subList(this.numSteps + 1, this.stepFloodNext.size()).clear();
        }
        // add stepColor
        this.stepColor.add(col);
        // add stepFlooded
        @SuppressWarnings("unchecked")
        final HashSet<ColorArea> flooded = (HashSet<ColorArea>) this.stepFlooded.get(this.numSteps).clone();
        flooded.addAll(newFlood);
        this.stepFlooded.add(flooded);
        // add stepFloodNext
        @SuppressWarnings("unchecked")
        final HashSet<ColorArea> floodNext = (HashSet<ColorArea>) this.stepFloodNext.get(this.numSteps).clone();
        floodNext.removeAll(newFlood);
        for (final ColorArea ca : newFlood) {
            for (final ColorArea caN : ca.getNeighborsArray()) {
                if (false == flooded.contains(caN)) {
                    floodNext.add(caN);
                }
            }
        }
        this.stepFloodNext.add(floodNext);
        // next step
        ++ this.numSteps;
        if ((true == this.isModifiable) && (false == init)) { // user added step
            GamePreferences.saveSolution(this);
        }
        return true;
    }

    /**
     * get the current colors of all cells.
     * the colors can either be the current flood color (if cell is
     * already flooded) or the original color of the board cell.
     * @return array of color numbers
     */
    public int[] getColors() {
        final int[] result = new int[this.board.getSize()];
        final Set<ColorArea> flooded = this.stepFlooded.get(this.numSteps);
        final int floodColor = this.stepColor.get(this.numSteps).intValue();
        for (int i = 0;  i < result.length;  ++i) {
            result[i] = this.board.getColor(i);
            for (final ColorArea ca : flooded) {
                if (ca.containsMember(i)) {
                    result[i] = floodColor;
                    break; // for (ca)
                }
            }
        }
        return result;
    }

    /**
     * check if undo is possible
     * @return true if undo is possible
     */
    public boolean canUndoStep() {
        return this.numSteps > 0;
    }

    /**
     * undo a color step.
     * @return true if step undo was successful
     */
    public boolean undoStep() {
        if (this.canUndoStep()) {
            -- this.numSteps;
            if (true == this.isModifiable) { // user solution
                GamePreferences.saveSolution(this);
            }
            return true;
        }
        return false;
    }

    /**
     * check if redo is possible
     * @return true if redo is possible
     */
    public boolean canRedoStep() {
        return this.numSteps < this.stepColor.size() - 1;
    }

    /**
     * redo a color step.
     * @return true if step redo was successful
     */
    public boolean redoStep() {
        if (this.canRedoStep()) {
            ++ this.numSteps;
            if (true == this.isModifiable) { // user solution
                GamePreferences.saveSolution(this);
            }
            return true;
        }
        return false;
    }

    /**
     * return true if the cell specified by index belongs to a neighbor
     * color of the flooded area.
     * @param index of board cell
     * @return true if cell can be flooded in the next step
     */
    public boolean isFloodNeighborCell(int index) {
        for (final ColorArea ca : this.stepFloodNext.get(this.numSteps)) {
            if (ca.containsMember(index)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return a collection of all cells that have the specified color
     * and that belong to a neighbor area of the flooded area.
     * @param color the color
     * @return collection of board cells
     */
    public Collection<Integer> getFloodNeighborCells(final int color) {
        final ArrayList<Integer> result = new ArrayList<Integer>();
        for (final ColorArea ca : this.stepFloodNext.get(this.numSteps)) {
            if (ca.getColor() == color) {
                result.addAll(ca.getMembers());
            }
        }
        return result;
    }

    /**
     * is the specified color in the neighbor area of the flooded area
     * and is it completed?  (i.e. will all cells of this color be flooded?)
     * @param color the color
     * @return true if the color will be flooded completely
     */
    public boolean isFloodNeighborCellsCompleted(final int color) {
        boolean result = true;
        final HashSet<ColorArea> flooded = this.stepFlooded.get(this.numSteps);
        final HashSet<ColorArea> floodNext = this.stepFloodNext.get(this.numSteps);
        for (final ColorArea ca : this.board.getColorAreas()) {
            if (ca.getColor() == color) {
                if (!flooded.contains(ca) && !floodNext.contains(ca)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * is the specified color deferrable?
     * which means that it has no neighbors outside of the known area (current flooded and neighbors)
     * and therefore this move would not expand the known neighbors (frontier) area.
     * @param color the color
     * @return true if the color is deferrable
     */
    public boolean isFloodNeighborCellsDeferrable(final int color) {
        final HashSet<ColorArea> nextFlooded = new HashSet<>(this.stepFlooded.get(this.numSteps));
        nextFlooded.addAll(this.stepFloodNext.get(this.numSteps));
        for (final ColorArea ca : this.stepFloodNext.get(this.numSteps)) {
            if (ca.getColor() == color
                    && nextFlooded.containsAll(ca.getNeighbors()) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * return the collection of cells matching the specified BoardColorNumbersEnum.
     * @param bcne
     * @return collection of board cells
     */
    public Collection<Integer> getBoardColorNumbers(final BoardColorNumbersEnum bcne) {
        final Collection<Integer> collectionColorNumbers;
        if (BoardColorNumbersEnum.ALL == bcne) {
            collectionColorNumbers = this.getNonFloodedCells();
        } else if (BoardColorNumbersEnum.NEXT == bcne) {
            collectionColorNumbers = this.getFloodNeighborCells();
        } else {
            collectionColorNumbers = Collections.emptyList();
        }
        return collectionColorNumbers;
    }

    /**
     * return a collection of all cells that belong to a neighbor area of the flooded area.
     * @return collection of board cells
     */
    public Collection<Integer> getFloodNeighborCells() {
        final ArrayList<Integer> result = new ArrayList<Integer>();
        for (final ColorArea ca : this.stepFloodNext.get(this.numSteps)) {
            result.addAll(ca.getMembers());
        }
        return result;
    }

    /**
     * return a collection of all cells that don't belong to the flooded area.
     * @return collection of board cells
     */
    public Collection<Integer> getNonFloodedCells() {
        final ArrayList<Integer> result = new ArrayList<Integer>();
        for (final ColorArea ca : this.board.getColorAreas()) {
            if (false == this.stepFlooded.get(this.numSteps).contains(ca)) {
                result.addAll(ca.getMembers());
            }
        }
        return result;
    }

    /**
     * return the current step number in this game progress.
     * @return current step
     */
    public int getCurrentStep() {
        return this.numSteps;
    }

    /**
     * return the total number of steps in this game progress.
     * @return total number of steps
     */
    public int getTotalSteps() {
        return this.stepColor.size() - 1;
    }

    /**
     * return true if the game is finished.
     * @return true if there are no steps left to do
     */
    public boolean isFinished() {
        return this.stepFloodNext.get(this.numSteps).isEmpty();
    }

    /**
     * get the next (upcoming) color from this game progress.
     * @return next color value or null if there is no next color
     */
    public Integer getNextColor() {
        if (this.canRedoStep()) {
            return this.stepColor.get(this.numSteps + 1);
        } else {
            return null;
        }
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name + " " + this.getTotalSteps();
    }

    public String toStringSteps() {
        final StringBuilder sb = new StringBuilder();
        for (final Integer color : this.stepColor) {
            sb.append(color.intValue() + 1);
        }
        return sb.toString();
    }

    public Board getBoard() {
        return this.board;
    }
}
