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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
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

    private Board board;
    private int startPos;

    private final GamePreferences pref;
    private volatile GameProgress progressUser;
    private volatile GameProgress progressSelected;

    private boolean isAutoRunSolver;
    private final AtomicReference<SolverRun> activeSolverRun = new AtomicReference<>();
    private final List<GameProgress> progressSolutions = new ArrayList<GameProgress>();
    public static final String PROPERTY_PROGRESS_SOLUTIONS = "progressSolutions";

    public GameState() {
        this.pref = new GamePreferences();
        this.setAutoRunSolver(false);
        this.initBoard();
    }

    private void initBoard() {
        this.board = new Board(this.pref.getWidth(), this.pref.getHeight(), this.pref.getNumColors());
        this.startPos = this.pref.getStartPos();
        this.board.determineColorAreasDepth(this.startPos);
        this.progressUser = new GameProgress(this.board, this.startPos);
        this.progressSelected = this.progressUser;
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

    public GameProgress getSelectedProgress() {
        return this.progressSelected;
    }

    public Board getBoard() {
        return this.board;
    }

    public GamePreferences getPreferences() {
        return this.pref;
    }

    /**
     * create a new board with random cell color values.
     */
    public void setNewRandomBoard() {
        this.initBoard();
    }

    /**
     * select a game progress.
     * @param numProgress number of the selected solution (0 == user solution, other = solver solutions)
     * @return true is game progress was selected
     */
    public boolean selectGameProgress(final int numProgress) {
        boolean isDone = true;
        if (0 == numProgress) {
            this.progressSelected = this.progressUser;
        } else {
            synchronized (this.progressSolutions) {
                if ((1 <= numProgress) && (this.progressSolutions.size() >= numProgress)) {
                    this.progressSelected = this.progressSolutions.get(numProgress - 1);
                } else {
                    isDone = false;
                }
            }
        }
        return isDone;
    }

    /**
     * is the currently selected game progress the one owned by the user?
     * @return true if user progress is selected
     */
    public boolean isUserProgress() {
        return this.progressSelected == this.progressUser; // use "==" here instead of "equals()"
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
