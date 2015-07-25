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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import colorfill.solver.DfsSolver;
import colorfill.solver.Solution;
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
    private final AtomicReference<SolverRun> activeSolverRun = new AtomicReference<SolverRun>();
    private final List<GameProgress> progressSolutions = new ArrayList<GameProgress>();
    public static final String PROPERTY_PROGRESS_SOLUTIONS = "progressSolutions";

    private final AtomicReference<GameState> hintGameState = new AtomicReference<GameState>();
    public static final String PROPERTY_HINT = "hint";

    private static final int NUMBER_OF_SOLVER_THREADS = 4;


    public GameState() {
        this.pref = new GamePreferences();
        this.setAutoRunSolver(false);
        this.initBoard(true);
    }

    private GameState(final GameState other) { // "hint" constructor
        this.pref = other.pref;
        this.startPos = other.startPos;
        this.board = new Board(other.progressUser); // copy Board and apply the steps from progressUser
    }

    private void initBoard(final boolean initialLoad) {
        this.board = null;
        this.progressUser = null;
        if (initialLoad) { // load board
            this.board = GamePreferences.loadBoard();
        }
        if (null != this.board) { // board loaded
            this.startPos = this.board.getStartPos();
            this.progressUser = GamePreferences.loadSolution(this.board, this.startPos);
        } else { // board not loaded
            this.board = new Board(this.pref.getWidth(), this.pref.getHeight(), this.pref.getNumColors());
            this.startPos = this.pref.getStartPos(this.pref.getWidth(), this.pref.getHeight());
            this.board.determineColorAreasDepth(this.startPos);
            GamePreferences.saveBoard(this.board);
        }
        if (null == this.progressUser) { // solution not loaded
            this.progressUser = new GameProgress(this.board, this.startPos);
            GamePreferences.saveSolution(this.progressUser);
        }
        this.progressSelected = this.progressUser;
        if (this.isAutoRunSolver) {
            new SolverRun(Integer.MAX_VALUE); // use all available solver strategies
        }
    }

    public void setAutoRunSolver(final boolean isAutoRunSolver) {
        final boolean oldValue = this.isAutoRunSolver;
        this.isAutoRunSolver = isAutoRunSolver;
        if ((false == oldValue) && (true == isAutoRunSolver)) {
            new SolverRun(Integer.MAX_VALUE); // use all available solver strategies
        }
    }

    public GameProgress getSelectedProgress() {
        return this.progressSelected;
    }

    public Board getBoard() {
        return this.board;
    }

    public int getStartPos() {
        return this.startPos;
    }

    public GamePreferences getPreferences() {
        return this.pref;
    }

    /**
     * create a new board with random cell color values.
     */
    public void setNewRandomBoard() {
        this.initBoard(false);
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
        private final int startPos, numberOfSolverStrategies;

        private SolverRun(final int numberOfSolverStrategies) {
            super();
            this.board = GameState.this.board;
            this.startPos = GameState.this.startPos;
            this.numberOfSolverStrategies = numberOfSolverStrategies;
            final SolverRun other = GameState.this.activeSolverRun.getAndSet(this);
            if ((null != other) && (this != other)) {
                other.interrupt();
            }
            this.start();
        }

        @Override
        public void run() {
            GameState.this.clearProgressSolutions();
            final ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_SOLVER_THREADS);
            final List<Future<Solution>> futureSolutions = new ArrayList<Future<Solution>>();
            final Class<Strategy>[] strategies = new DfsSolver(this.board).getSupportedStrategies();
            int i = 0;
            for (final Class<Strategy> strategy : strategies) {
                if (++i > this.numberOfSolverStrategies) {
                    break; // for()
                }
                final Solver solver = new DfsSolver(this.board);
                solver.setStrategy(strategy);
                futureSolutions.add(executor.submit(new Callable<Solution>() {
                    public Solution call() throws Exception {
                        solver.execute(SolverRun.this.startPos);
                        return solver.getSolution();
                    }
                }));
            }
            for (final Future<Solution> futureSolution : futureSolutions) {
                Solution solution = null;
                try {
                    solution = futureSolution.get();
                } catch (InterruptedException e) {
                    System.out.println("***** SolverRun interrupted *****");
                    executor.shutdownNow(); // interrupt the solver threads
                } catch (ExecutionException e) {
                    if (false == e.getCause() instanceof InterruptedException) {
                        e.printStackTrace();
                    }
                } catch (CancellationException e) {
                    // do nothing
                }
                if (null != solution) {
                    GameState.this.addProgressSolution(new GameProgress(this.board, this.startPos, solution));
                    System.out.println(
                            padRight(solution.getSolverName(), 21 + 2) // 21==max. length of strategy names
                            + padRight("steps(" + solution.getNumSteps() + ")", 7 + 2 + 2)
                            + "solution(" + solution + ")");
                }
            }
            executor.shutdown();
            System.out.println();
            GameState.this.activeSolverRun.compareAndSet(this, null);
            GameState.this.firePropertyChange(GameState.PROPERTY_HINT, null, null); // callback to the "main" GameState.calculateHint()
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



    public void calculateHint() {
        final GameState newHint = new GameState(this);
        newHint.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (GameState.PROPERTY_HINT.equals(evt.getPropertyName())) {
                    Integer color = null, stepsToDo = Integer.valueOf(Integer.MAX_VALUE);
                    for (final GameProgress gp : newHint.progressSolutions) {
                        if (stepsToDo.intValue() > gp.getTotalSteps()) {
                            stepsToDo = Integer.valueOf(gp.getTotalSteps());
                            color = gp.getNextColor();
                        }
                    }
                    if (null != color) {
                        final Integer estimatedSteps = Integer.valueOf(GameState.this.progressUser.getCurrentStep() + stepsToDo.intValue());
                        GameState.this.firePropertyChange(GameState.PROPERTY_HINT, color, estimatedSteps); // notify GUI controller
                    }
                }
            }
        });
        this.setHint(newHint);
        newHint.calculateHintSolver();
    }

    public void removeHint() {
        this.setHint(null);
    }

    private void setHint(final GameState newHint) {
        final GameState oldHint = this.hintGameState.getAndSet(newHint);
        if (null != oldHint) {
            final SolverRun oldSolver = oldHint.activeSolverRun.getAndSet(null);
            if (null != oldSolver) {
                oldSolver.interrupt();
            }
        }
    }

    private void calculateHintSolver() {
        System.out.println("calculateHintSolver");
        new SolverRun(2); // use only the 2 fastest solver strategies
    }



    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }
}
