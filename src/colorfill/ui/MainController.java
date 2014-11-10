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

package colorfill.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import colorfill.model.GameProgress;
import colorfill.model.GameState;

/**
 * the main controller of the GUI, coordinates the control flows
 * of all views (panels).
 */
public class MainController {

    private GameState gameState;

    private MainWindow mainView;
    private BoardController boardController;
    private ControlController controlController;

    /**
     * the main entry point to this Swing GUI.
     * @param windowTitle title text of the application window
     */
    public MainController(final String windowTitle) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(windowTitle);
            }
        });
    }

    private void createAndShowGUI(final String windowTitle) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.gameState = new GameState();
        this.gameState.addPropertyChangeListener(new GameStatePropertyChangeListener());
        this.boardController = new BoardController(this, this.gameState);
        this.controlController = new ControlController(this, this.gameState);
        this.mainView = new MainWindow(windowTitle, this.boardController.getPanel(), this.controlController.getPanel());
        this.mainView.update();
        this.gameState.setAutoRunSolver(true);
    }

    /**
     * add a color step to gamestate.
     * @param color
     */
    protected void actionAddStep(final int color) {
        final boolean isAdded = this.gameState.addStep(color);
        if (isAdded) {
            this.boardController.actionUpdateBoardColors();
            this.controlController.actionUpdateBoardColors();
        }
    }

    /**
     * undo a color step in gamestate.
     */
    protected void actionUndoStep() {
        final boolean isDone = this.gameState.undoStep();
        if (isDone) {
            this.boardController.actionUpdateBoardColors();
            this.controlController.actionUpdateBoardColors();
        }
    }

    /**
     * redo a color step in gamestate.
     */
    protected void actionRedoStep() {
        final boolean isDone = this.gameState.redoStep();
        if (isDone) {
            this.boardController.actionUpdateBoardColors();
            this.controlController.actionUpdateBoardColors();
        }
    }

    /**
     * make a new board with random cell colors.
     */
    protected void actionNewBoard() {
        this.gameState.setNewRandomBoard();
        this.boardController.actionUpdateBoardColors();
        this.controlController.actionUpdateBoardColors();
    }

    /**
     * this class handles the Property Change Events coming from GameState
     * when the solver(s) running in a worker thread present their solutions.
     */
    private class GameStatePropertyChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if (GameState.PROPERTY_PROGRESS_SOLUTIONS.equals(evt.getPropertyName())) {
//                final GameProgress[] oldValue = (GameProgress[]) evt.getOldValue();
                final GameProgress[] newValue = (GameProgress[]) evt.getNewValue();
                if (0 == newValue.length) {
                    MainController.this.controlController.actionClearSolverResults();
                } else {
//                    System.out.println("propertyChange add " + (newValue.length - oldValue.length));
                    MainController.this.controlController.actionAddSolverResult(newValue[newValue.length - 1]);
                }
            }
        }
    }
}
