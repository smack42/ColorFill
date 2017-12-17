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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import colorfill.solver.AStarTigrouStrategy;
import colorfill.solver.AbstractSolver;
import colorfill.solver.DfsDeepStrategy;
import colorfill.solver.DfsDeeperStrategy;
import colorfill.solver.DfsExhaustiveStrategy;
import colorfill.solver.DfsGreedyNextStrategy;
import colorfill.solver.DfsGreedyStrategy;
import colorfill.solver.Solution;
import colorfill.solver.Solver;
import colorfill.solver.Strategy;

/**
 * this class represents the current state of the game.
 * it is the model of the GUI.
 */
public class GameState {

    private static final Class<?>[] STRATEGIES = { // all solver strategies, sorted by average speed (fastest first)
        AStarTigrouStrategy.class
        ,DfsGreedyStrategy.class
        ,DfsGreedyNextStrategy.class
        ,DfsDeepStrategy.class
        ,DfsDeeperStrategy.class
        ,DfsExhaustiveStrategy.class // DfsExhaustiveStrategy must be the last entry in this array!
    };

    private static final String[] SOLVER_NAMES = new String[STRATEGIES.length];
    static {
        for (int i = 0;  i < SOLVER_NAMES.length;  ++i) {
            SOLVER_NAMES[i] = AbstractSolver.getSolverName((Class<Strategy>) STRATEGIES[i]);
        }
    }

    private Board board;
    private int startPos;

    private final GamePreferences pref;
    private volatile GameProgress progressUser;
    private volatile GameProgress progressSelected;

    private boolean isAutoRunSolver;
    private final AtomicReference<SolverRun> activeSolverRun = new AtomicReference<SolverRun>();
    private final GameProgress[] progressSolutions = new GameProgress[STRATEGIES.length];
    public static final String PROPERTY_PROGRESS_SOLUTIONS = "progressSolutions";

    private final AtomicReference<GameState> hintGameState = new AtomicReference<GameState>();
    public static final String PROPERTY_HINT = "hint";

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

    public String[] getSolverNames() {
        return SOLVER_NAMES;
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
                final int i = numProgress - 1;
                if ((0 <= i) && (this.progressSolutions.length > i) && (null != this.progressSolutions[i])) {
                    this.progressSelected = this.progressSolutions[i];
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

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            GameState.this.clearProgressSolutions();
            final int numThreads = Math.min(Runtime.getRuntime().availableProcessors() * 2, this.numberOfSolverStrategies);
            final ThreadFactory threadFactory = new ThreadFactory() {
                final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                @Override
                public Thread newThread(Runnable r) {
                    final Thread t = this.defaultFactory.newThread(r);
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                }
            };
            final ExecutorService executor = Executors.newFixedThreadPool(numThreads, threadFactory);
            final List<Future<Solution>> futureSolutions = new ArrayList<Future<Solution>>();
            int strategyIdx;
            for (strategyIdx = 0;  strategyIdx < STRATEGIES.length;  ++strategyIdx) {
                if (strategyIdx >= this.numberOfSolverStrategies) {
                    strategyIdx = Integer.MAX_VALUE;
                    break; // for()
                }
                // DfsExhaustiveStrategy must be the last entry in this array!
                if (DfsExhaustiveStrategy.class.equals(STRATEGIES[strategyIdx])) {
                    break; // for()
                }
                final Solver solver = AbstractSolver.createSolver((Class<Strategy>)STRATEGIES[strategyIdx], this.board);
                futureSolutions.add(executor.submit(new Callable<Solution>() {
                    public Solution call() throws Exception {
                        try {
                            solver.execute(SolverRun.this.startPos, null);
                            return solver.getSolution();
                        } finally {
                            final String info = solver.getSolverInfo();
                            if ((null != info) && (0 != info.length())) {
                                System.out.println(info);
                            }
                        }
                    }
                }));
            }
            Solution bestSolution = null;
            boolean interrupted = false;
            for (int waitMask = (1 << futureSolutions.size()) - 1;  waitMask != 0;  ) {
                for (int i = 0;  i < futureSolutions.size();  ++i) {
                    final int iMask = (1 << i); // bit for this solution
                    if (0 != (waitMask & iMask)) {
                        waitMask ^= iMask; // clear this bit, expecting to get the solution or an exception
                        Solution solution = null;
                        try {
                            solution = futureSolutions.get(i).get(0 == waitMask ? 5000 : 50, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            System.out.println("***** SolverRun interrupted *****");
                            interrupted = true;
                            executor.shutdownNow(); // interrupt the solver threads
                            waitMask = 0; // end outer loop
                            break; // end inner loop
                        } catch (ExecutionException e) {
                            if (false == e.getCause() instanceof InterruptedException) {
                                e.printStackTrace();
                            }
                            solution = new Solution(new byte[0], SOLVER_NAMES[i]);
                        } catch (CancellationException e) {
                            // do nothing
                        } catch (TimeoutException e) {
                            waitMask |= iMask; // set this bit again because the solution is not ready yet
                        }
                        if (null != solution) {
                            if ((null == bestSolution) || (solution.getNumSteps() < bestSolution.getNumSteps())) {
                                bestSolution = solution;
                            }
                            GameState.this.addProgressSolution(new GameProgress(this.board, this.startPos, solution));
                            System.out.println(
                                    padRight(solution.getSolverName(), 21 + 2) // 21==max. length of strategy names
                                    + padRight("steps(" + solution.getNumSteps() + ")", 7 + 2 + 2)
                                    + "solution(" + solution + ")");
                        }
                    }
                }
            }
            executor.shutdown();
            // run DfsExhaustiveStrategy now
            if (!interrupted && (strategyIdx < STRATEGIES.length) && DfsExhaustiveStrategy.class.equals(STRATEGIES[strategyIdx])) {
                final Solver solver = AbstractSolver.createSolver((Class<Strategy>)STRATEGIES[strategyIdx], this.board);
                Solution solution = null;
                try {
                    solver.execute(SolverRun.this.startPos, bestSolution);
                    solution = solver.getSolution();
                } catch (InterruptedException e) {
                    System.out.println("***** SolverRun interrupted *****");
                } catch (Throwable e) {
                    if (false == e.getCause() instanceof InterruptedException) {
                        e.printStackTrace();
                    }
                    solution = new Solution(new byte[0], SOLVER_NAMES[strategyIdx]);
                } finally {
                    final String info = solver.getSolverInfo();
                    if ((null != info) && (0 != info.length())) {
                        System.out.println(info);
                    }
                    if (null != solution) {
                        GameState.this.addProgressSolution(new GameProgress(this.board, this.startPos, solution));
                        System.out.println(
                                padRight(solution.getSolverName(), 21 + 2) // 21==max. length of strategy names
                                + padRight("steps(" + solution.getNumSteps() + ")", 7 + 2 + 2)
                                + "solution(" + solution + ")");
                    }
                }
            }
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



    private void clearProgressSolutions() {
        final Object oldValue = null, newValue;
        synchronized (this.progressSolutions) {
            Arrays.fill(this.progressSolutions, null);
            newValue = null;
        }
        this.firePropertyChange(PROPERTY_PROGRESS_SOLUTIONS, oldValue, newValue);
    }
    private void addProgressSolution(final GameProgress progress) {
        final Object oldValue = null, newValue;
        synchronized (this.progressSolutions) {
            final String solverName = progress.getName();
            int i;
            for (i = 0;  i < SOLVER_NAMES.length;  ++i) {
                if (SOLVER_NAMES[i].equals(solverName)) {
                    break;
                }
            }
            if (i < SOLVER_NAMES.length) { // solverName found
                this.progressSolutions[i] = progress;
                newValue = progress;
            } else { // solverName not found
                newValue = null;
            }
        }
        if (null != newValue) { // solverName found
            this.firePropertyChange(PROPERTY_PROGRESS_SOLUTIONS, oldValue, newValue);
        }
    }



    public void calculateHint() {
        final GameState newHint = new GameState(this);
        newHint.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (GameState.PROPERTY_HINT.equals(evt.getPropertyName())) {
                    Integer color = null, stepsToDo = Integer.valueOf(Integer.MAX_VALUE);
                    for (final GameProgress gp : newHint.progressSolutions) {
                        if ((null != gp) && (stepsToDo.intValue() > gp.getTotalSteps())) {
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
        new SolverRun(3); // use only the 3 fastest solver strategies
    }






    /**
     * try to parse the specified gameId and return the resulting GameState
     * @param gameId
     * @return the resulting GameState or IllegalArgumentException if something went wrong
     */
    public static GameState tryInfoGameId(String gameId) throws IllegalArgumentException {
        final GameState gs = new GameState();
        final String error = gs.internalApplyGameId(gameId, false, false);
        if ((null == error) || (0 == error.length())) {
            return gs;
        } else {
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * compare the specified gameId with the current GameState
     * @param gameId
     * @return true if gameId is equal to current GameState
     */
    public boolean isSameGameId(String gameId) {
        boolean result = false;
        if (null != gameId) {
            gameId = gameId.replaceAll("\\s", ""); //remove whitespace
            result = this.getGameId().equals(gameId);
        }
        return result;
    }

    private static final String GAMEID_PREFIX = "ColorFill";
    private static final String GAMEID_SEPARATOR = ";";

    /**
     * return the game ID that represents the current GameState
     * @return gameId
     */
    public String getGameId() {
        final StringBuilder sb = new StringBuilder();
        sb  .append(GAMEID_SEPARATOR).append(GAMEID_PREFIX)
            .append(GAMEID_SEPARATOR).append(this.board.getWidth())
            .append(GAMEID_SEPARATOR).append(this.board.getHeight())
            .append(GAMEID_SEPARATOR).append(this.board.getNumColors())
            .append(GAMEID_SEPARATOR).append(StartPositionEnum.intValueFromPosition(this.board.getStartPos(), this.board.getWidth(), this.board.getHeight()))
            .append(GAMEID_SEPARATOR).append(this.board.toStringCells())
            .append(GAMEID_SEPARATOR).append(this.progressUser.getCurrentStep())
            .append(GAMEID_SEPARATOR).append(this.progressUser.toStringSteps())
            .append(GAMEID_SEPARATOR);
        return sb.toString();
    }

    /**
     * try to parse the the specified gameId and to apply its
     * contents to this GameState
     * @param gameId
     * @return error message (null or empty means "OK")
     */
    public String applyGameId(final String gameId) {
        return internalApplyGameId(gameId, true, true);
    }

    private String internalApplyGameId(String gameId, final boolean doSolver, final boolean doPrefs) {
        if (null == gameId) {
            return "null";
        }
        gameId = gameId.replaceAll("\\s", ""); //remove whitespace
        if (gameId.isEmpty()) {
            return "empty";
        }
        try {
            final int width, height, numColors, speIntValue, startPos, currentStep;
            final String cells, steps;
            // try to parse it as a ColorFill GameID
            final String[] str = gameId.split(GAMEID_SEPARATOR);
            if ((8+1 == str.length) && (0 == str[0].length()) && GAMEID_PREFIX.equals(str[1])) {
                int i = 2;
                width = Integer.parseInt(str[i++]);
                height = Integer.parseInt(str[i++]);
                numColors = Integer.parseInt(str[i++]);
                speIntValue = Integer.parseInt(str[i++]);
                startPos = StartPositionEnum.calculatePosition(speIntValue, width, height);
                cells = str[i++];
                currentStep = Integer.parseInt(str[i++]);
                steps = str[i++];
            } else {
                // try to parse it as a "Flood" GameID
                // from Simon Tatham's Portable Puzzle Collection
                // format: width + "x" + height [+ "c" + numColors] [+ "m" + extraMoves] + ":" + cells(0...c) + "," + numMoves
                // 14x14:5034352442000401554521300213305402320310535452020550552422052045403530325333450525112112303133345114253150435533444400020013402304412530111000404130225111421100032452435511440141531031030511452454,20
                final String WITH_DELIMITER = "((?<=%1$s)|(?=%1$s))";
                final String[] strFlood = gameId.split(String.format(WITH_DELIMITER, "[xcm:,]"));
                width = Integer.parseInt(strFlood[0]);
                height = Integer.parseInt(strFlood[2]); // expect "x" at strFlood[1]
                // find cells string
                String strFloodCells = null;
                for (int i = 0;  i < strFlood.length;  ++i) {
                    if (":".equals(strFlood[i])) {
                        strFloodCells = strFlood[i+1];
                        break;
                    }
                }
                // transform cells string from (0...c) to (1...c+1)
                final StringBuilder sbCells = new StringBuilder();
                int maxColor = 0;
                if (null != strFloodCells) for (int i = 0;  i < strFloodCells.length();  ++i) {
                    final char c = strFloodCells.charAt(i);
                    maxColor = Math.max(maxColor, Character.digit(c, 10));
                    sbCells.append((char)(c + 1));
                }
                cells = sbCells.toString();
                numColors = maxColor + 1;
                speIntValue = StartPositionEnum.TOP_LEFT.intValue;
                startPos = 0;
                currentStep = 0;
                steps = null;
            }
            if (width * height != cells.length()) {
                throw new IllegalArgumentException("w=" + width + " h=" + height + " c=" + cells.length());
            }
            if ((2 > numColors) || (6 < numColors)) {
                throw new UnsupportedOperationException("colors=" + numColors);
            }
            if ((0 > currentStep) || ((null != steps) && (steps.length() <= currentStep))) {
                throw new IllegalArgumentException("currentStep=" + currentStep);
            }
            final Board b = new Board(width, height, numColors, cells, startPos);
            final GameProgress gp = new GameProgress(b, startPos, currentStep, steps);
            // update GameState
            this.board = b;
            this.startPos = startPos;
            this.progressUser = gp;
            this.selectGameProgress(0);
            this.removeHint();
            if (doSolver) {
                new SolverRun(Integer.MAX_VALUE); // use all available solver strategies
            }
            // update GamePreferences
            if (doPrefs) {
                this.pref.setWidth(b.getWidth());
                this.pref.setHeight(b.getHeight());
                this.pref.setNumColors(b.getNumColors());
                this.pref.setStartPos(speIntValue);
                this.pref.savePrefs();
                GamePreferences.saveBoard(b);
                GamePreferences.saveSolution(gp);
            }
        } catch (final Exception e) {
            return "not a valid GameID; " + e.toString();
        }
        return null;
    }



    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }
}
