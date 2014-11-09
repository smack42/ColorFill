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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

    private GameProgress progressUser;

    private boolean isAutoRunSolver;
    private final AtomicReference<SolverRun> activeSolverRun = new AtomicReference<>();
    private final List<GameProgress> progressSolutions = new ArrayList<GameProgress>();
    public static final String PROPERTY_PROGRESS_SOLUTIONS = "progressSolutions";

    public GameState() {
        this.prefWidth = DEFAULT_BOARD_WIDTH;
        this.prefHeight = DEFAULT_BOARD_HEIGHT;
        this.prefNumColors = DEFAULT_BOARD_NUM_COLORS;
        this.prefStartPos = DEFAULT_BOARD_STARTPOS;
        this.prefUiColors = DEFAULT_UI_COLORS;
        this.setAutoRunSolver(false);
        this.initBoard();
    }

    private void initBoard() {
        this.board = new Board(this.prefWidth, this.prefHeight, this.prefNumColors);
        this.startPos = this.prefStartPos;
        this.board.determineColorAreasDepth(this.startPos);
        this.progressUser = new GameProgress(this.board, this.startPos);
        if (this.isAutoRunSolver) {
            new SolverRun(this.board, this.startPos);
        }
    }

    public void setAutoRunSolver(final boolean isAutoRunSolver) {
        final boolean oldValue = this.isAutoRunSolver;
        this.isAutoRunSolver = isAutoRunSolver;
        if ((false == oldValue) && (true == isAutoRunSolver)) {
            new SolverRun(this.board, this.startPos);
        }
    }

    /**
     * get the current colors of all cells.
     * the colors can either be the current flood color (if cell is
     * already flooded) or the original color of the board cell.
     * @return array of color numbers
     */
    public int[] getColors() {
        // TODO use solver solutions
        return this.progressUser.getColors();
    }

    public Board getBoard() {
        return this.board;
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

    public int getCurrentStep() {
        // TODO use solver solutions
        return this.progressUser.getCurrentStep();
    }

    public boolean isFinished() {
        // TODO use solver solutions
        return this.progressUser.isFinished();
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
        // TODO use solver solutions
        return this.progressUser.addStep(color);
    }

    /**
     * check if undo is possible
     * @return true if undo is possible
     */
    public boolean canUndoStep() {
        // TODO use solver solutions
        return this.progressUser.canUndoStep();
    }

    /**
     * undo a color step.
     * @return true if step undo was successful
     */
    public boolean undoStep() {
        // TODO use solver solutions
        return this.progressUser.undoStep();
    }

    /**
     * check if redo is possible
     * @return true if redo is possible
     */
    public boolean canRedoStep() {
        // TODO use solver solutions
        return this.progressUser.canRedoStep();
    }

    /**
     * redo a color step.
     * @return true if step redo was successful
     */
    public boolean redoStep() {
        // TODO use solver solutions
        return this.progressUser.redoStep();
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
        // TODO use solver solutions
        return this.progressUser.isFloodNeighborCell(index);
    }

    /**
     * return a collection of all cells that have the specified color
     * and that belong to a neighbor area of the flooded area.
     * @param color the color
     * @return collection of board cells
     */
    public Collection<Integer> getFloodNeighborCells(final int color) {
        // TODO use solver solutions
        return this.progressUser.getFloodNeighborCells(color);
    }



    private class SolverRun extends Thread {
        private final Board board;
        private final int startPos;

        private SolverRun(final Board board, final int startPos) {
            super();
            this.board = board;
            this.startPos = startPos;
            final SolverRun other = GameState.this.activeSolverRun.getAndSet(this);
            if ((null != other) && (this != other)) {
                other.interrupt();
            }
            this.start();
        }

        @Override
        public void run() {
            GameState.this.clearProgressSolutions();
            final Solver solver = new DfsSolver(this.board);
            final Class<Strategy>[] strategies = solver.getSupportedStrategies();
            for (final Class<Strategy> strategy : strategies) {
                solver.setStrategy(strategy);
                final long nanoStart = System.nanoTime();
                try {
                    solver.execute(this.startPos);
                } catch (InterruptedException e) {
                    System.out.println("***** SolverRun interrupted *****");
                    break; // for (strategy)
                }
                final long nanoEnd = System.nanoTime();
                GameState.this.addProgressSolution(new GameProgress(this.board, this.startPos, solver.getSolution()));
                System.out.println(
                        padRight(strategy.getSimpleName(), 17 + 2) // 17==max. length of strategy names
                        + padRight("steps(" + solver.getSolution().getNumSteps() + ")", 7 + 2 + 2)
                        + padRight("ms(" + ((nanoEnd - nanoStart + 999999L) / 1000000L) + ")", 4 + 5 + 1)
                        + "solution(" + solver.getSolution() + ")");
            }
            System.out.println();
            GameState.this.activeSolverRun.compareAndSet(this, null);
        }
    }



    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
    private void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        this.propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }



    private static final GameProgress[] EMPTY_ARRAY_GAME_PROGRESS = new GameProgress[0];
    private void clearProgressSolutions() {
        final Object oldValue, newValue;
        synchronized (this.progressSolutions) {
            oldValue = this.progressSolutions.toArray(EMPTY_ARRAY_GAME_PROGRESS);
            this.progressSolutions.clear();
            newValue = this.progressSolutions.toArray(EMPTY_ARRAY_GAME_PROGRESS);
        }
        this.firePropertyChange(PROPERTY_PROGRESS_SOLUTIONS, oldValue, newValue);
    }
    private void addProgressSolution(final GameProgress progress) {
        final Object oldValue, newValue;
        synchronized (this.progressSolutions) {
            oldValue = this.progressSolutions.toArray(EMPTY_ARRAY_GAME_PROGRESS);
            this.progressSolutions.add(progress);
            newValue = this.progressSolutions.toArray(EMPTY_ARRAY_GAME_PROGRESS);
        }
        this.firePropertyChange(PROPERTY_PROGRESS_SOLUTIONS, oldValue, newValue);
    }



    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }
}
