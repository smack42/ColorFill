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

package colorfill.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import colorfill.solver.DfsSolver;
import colorfill.solver.Solver;
import colorfill.solver.Strategy;

/**
 * this class represents the current state of the game.
 * it is the model of the GUI.
 */
public class GameState {

    public static final int DEFAULT_BOARD_WIDTH  = 14;
    public static final int DEFAULT_BOARD_HEIGHT = 14;
    public static final int DEFAULT_BOARD_NUM_COLORS = 6;
    public static final int DEFAULT_BOARD_STARTPOS = 0; // 0 == top left corner

    private static final Color[] DEFAULT_UI_COLORS = {
        // Flood-It scheme
        new Color(0xDC4A20), // Color.RED
        new Color(0x7E9D1E), // Color.GREEN
        new Color(0x605CA8), // Color.BLUE
        new Color(0xF3F61D), // Color.YELLOW
        new Color(0x46B1E2), // Color.CYAN
        new Color(0xED70A1)  // Color.MAGENTA

        // Color Flood (Android) scheme 1 (default)
//        new Color(0x6261A8),
//        new Color(0x6AAECC),
//        new Color(0x5EDD67),
//        new Color(0xF66A61),
//        new Color(0xF6BF61),
//        new Color(0xF0F461)

        // Color Flood (Android) scheme 6
//        new Color(0xDF5162),
//        new Color(0x38322F),
//        new Color(0x247E86),
//        new Color(0x1BC4C1),
//        new Color(0xFCF8C9),
//        new Color(0xD19C2D)
    };

    private int prefWidth;
    private int prefHeight;
    private int prefNumColors;
    private int prefStartPos;
    private Color[] prefUiColors;

    private Board board;
    private int startPos;
    private int numSteps;
    private final List<Integer> stepColor = new ArrayList<>();
    private final List<HashSet<ColorArea>> stepFlooded = new ArrayList<>();
    private final List<HashSet<ColorArea>> stepFloodNext = new ArrayList<>();
    private final AtomicReference<SolverRun> activeSolverRun = new AtomicReference<>();

    public GameState() {
        this.prefWidth = DEFAULT_BOARD_WIDTH;
        this.prefHeight = DEFAULT_BOARD_HEIGHT;
        this.prefNumColors = DEFAULT_BOARD_NUM_COLORS;
        this.prefStartPos = DEFAULT_BOARD_STARTPOS;
        this.prefUiColors = DEFAULT_UI_COLORS;
        this.initBoard();
    }

    private void initBoard() {
        this.board = new Board(this.prefWidth, this.prefHeight, this.prefNumColors);
        this.startPos = this.prefStartPos;
        this.board.determineColorAreasDepth(this.startPos);
        this.numSteps = 0;
        this.stepColor.clear();
        this.stepColor.add(Integer.valueOf(this.board.getColor(this.startPos)));
        this.stepFlooded.clear();
        this.stepFlooded.add(new HashSet<ColorArea>(Collections.singleton(this.board.getColorArea(this.startPos))));
        this.stepFloodNext.clear();
        this.stepFloodNext.add(new HashSet<ColorArea>(this.board.getColorArea(this.startPos).getNeighbors()));
        new SolverRun(this.activeSolverRun, this.board, this.startPos);
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
            final Integer cell = Integer.valueOf(i);
            for (final ColorArea ca : flooded) {
                if (ca.getMembers().contains(cell)) {
                    result[i] = floodColor;
                    break; // for (ca)
                }
            }
        }
        return result;
    }

    public Board getBoard() {
        return this.board;
    }
    public void setBoard(Board board) {
        this.board = board;
    }

    public int getPrefWidth() {
        return this.prefWidth;
    }
    public void setPrefWidth(int width) {
        this.prefWidth = width;
    }

    public int getPrefHeight() {
        return this.prefHeight;
    }
    public void setPrefHeight(int height) {
        this.prefHeight = height;
    }

    public int getPrefNumColors() {
        return this.prefNumColors;
    }
    public void setPrefNumColors(int colors) {
        this.prefNumColors = colors;
    }

    public int getPrefStartPos() {
        return this.prefStartPos;
    }
    public void setPrefStartPos(int startPos) {
        this.prefStartPos = startPos;
    }

    public int getNumSteps() {
        return this.numSteps;
    }
    public boolean isFinished() {
        return this.stepFloodNext.get(this.numSteps).isEmpty();
    }

    public Color[] getPrefUiColors() {
        return Arrays.copyOf(this.prefUiColors, this.prefUiColors.length);
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
    public boolean addStep(int color) {
        final Integer col = Integer.valueOf(color);
        // check if same color as before or nothing to be flooded
        if (this.stepColor.get(this.numSteps).equals(col)
                || this.stepFloodNext.get(this.numSteps).isEmpty()) {
            return false;
        }
        // current lists are too long (because of undo) - remove the future moves
        if (this.stepColor.size() > this.numSteps + 1) {
            this.stepColor.subList(this.numSteps + 1, this.stepColor.size()).clear();
            this.stepFlooded.subList(this.numSteps + 1, this.stepFlooded.size()).clear();
            this.stepFloodNext.subList(this.numSteps + 1, this.stepFloodNext.size()).clear();
        }
        // add stepColor
        this.stepColor.add(col);
        final Set<ColorArea> newFlood = new HashSet<>();
        for (final ColorArea ca : this.stepFloodNext.get(this.numSteps)) {
            if (ca.getColor().equals(col)) {
                newFlood.add(ca);
            }
        }
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
            for (final ColorArea caN : ca.getNeighbors()) {
                if (false == flooded.contains(caN)) {
                    floodNext.add(caN);
                }
            }
        }
        this.stepFloodNext.add(floodNext);
        // next step
        ++ this.numSteps;
        return true;
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
            return true;
        }
        return false;
    }

    /**
     * create a new board with random cell color values.
     */
    public void setNewRandomBoard() {
        this.initBoard();
    }

    /**
     * return true if the cell specified by index belongs to a neighbor
     * color of the flooded area.
     * @param index of board cell
     * @return true if cell can be flooded in the next step
     */
    public boolean isFloodNeighborCell(int index) {
        final Integer cell = Integer.valueOf(index);
        for (final ColorArea ca : this.stepFloodNext.get(this.numSteps)) {
            if (ca.getMembers().contains(cell)) {
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
        final ArrayList<Integer> result = new ArrayList<>();
        for (final ColorArea ca : this.stepFloodNext.get(this.numSteps)) {
            if (ca.getColor().intValue() == color) {
                result.addAll(ca.getMembers());
            }
        }
        return result;
    }

    private static class SolverRun { // TODO return solution(s) to GameState
        private SolverRun(final AtomicReference<SolverRun> myExternalReference, final Board board, final int startPos) {
            myExternalReference.set(this);
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    final Solver solver = new DfsSolver(board);
                    final Class<Strategy>[] strategies = solver.getSupportedStrategies();
                    int strategyNameLength = 0;
                    for (final Class<Strategy> strategy : strategies) {
                        if (strategyNameLength < strategy.getSimpleName().length()) {
                            strategyNameLength = strategy.getSimpleName().length();
                        }
                    }
                    for (final Class<Strategy> strategy : strategies) {
                        solver.setStrategy(strategy);
                        final long nanoStart = System.nanoTime();
                        final int solutionSteps = solver.execute(startPos);
                        final long nanoEnd = System.nanoTime();
                        if (SolverRun.this != myExternalReference.get()) {
                            System.out.println("SolverRun BREAK");
                            return;
                        }
                        System.out.println(
                                padRight(strategy.getSimpleName(), strategyNameLength + 2)
                                + padRight("steps(" + solutionSteps + ")", 7 + 2 + 2)
                                + padRight("ms(" + ((nanoEnd - nanoStart + 999999L) / 1000000L) + ")", 4 + 5 + 1)
                                + "solution(" + solver.getSolutionString() + ")");
                    }
                    System.out.println();
                }
            });
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
        }
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }
}
