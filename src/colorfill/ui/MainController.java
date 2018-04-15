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

package colorfill.ui;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import colorfill.model.BoardColorNumbersEnum;
import colorfill.model.GameProgress;
import colorfill.model.GameState;
import colorfill.model.GridLinesEnum;
import colorfill.model.HighlightColorEnum;

/**
 * the main controller of the GUI, coordinates the control flows
 * of all views (panels).
 */
public class MainController {

    private GameState gameState;

    private MainWindow mainView;
    private BoardController boardController;
    private ControlController controlController;
    private PreferencesController preferencesController;
    private GameIdController gameidController;

    /**
     * the main entry point to this Swing GUI.
     * @param windowTitle title text of the application window
     */
    public MainController(final String progname, final String version, final String author) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(progname, version, author);
            }
        });
    }

    private void createAndShowGUI(final String progname, final String version, final String author) {
        try {
            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.gameState = new GameState();
        this.gameState.addPropertyChangeListener(new GameStatePropertyChangeListener());
        this.boardController = new BoardController(this, this.gameState);
        this.controlController = new ControlController(this, this.gameState);
        this.controlController.actionUpdateBoardColors();
        this.mainView = new MainWindow(progname, this.boardController.getPanel(), this.controlController.getPanel());
        this.mainView.update();
        this.gameState.setAutoRunSolver(true);
        this.preferencesController = new PreferencesController(this, this.gameState, this.mainView, progname, version, author);
        this.gameidController = new GameIdController(this, this.gameState, this.mainView);
    }

    private void internalUpdateBoardColors() {
        this.boardController.actionUpdateBoardColors();
        this.controlController.actionUpdateBoardColors();
        if (false == this.gameState.isUserProgress()) {
            final Integer nextColor = this.gameState.getSelectedProgress().getNextColor();
            if (null != nextColor) {
                this.boardController.actionHightlightFloodNeighborCells(nextColor.intValue());
            }
        }
    }

    /**
     * add a color step to gamestate.
     * @param color
     */
    protected void actionAddStep(final int color) {
        final boolean isAdded = this.gameState.getSelectedProgress().addStep(color);
        if (isAdded) {
            this.hideHint();
            this.internalUpdateBoardColors();
        }
    }

    /**
     * undo a color step in gamestate.
     */
    protected void actionUndoStep() {
        final boolean isDone = this.gameState.getSelectedProgress().undoStep();
        if (isDone) {
            if (this.gameState.isUserProgress()) {
                this.hideHint();
            }
            this.internalUpdateBoardColors();
        }
    }

    /**
     * redo a color step in gamestate.
     */
    protected void actionRedoStep() {
        final boolean isDone = this.gameState.getSelectedProgress().redoStep();
        if (isDone) {
            if (this.gameState.isUserProgress()) {
                this.hideHint();
            }
            this.internalUpdateBoardColors();
        }
    }

    /**
     * make a new board with random cell colors.
     */
    protected void actionNewBoard() {
        this.gameState.setNewRandomBoard();
        this.hideHint();
        this.internalUpdateBoardColors();
    }

    /**
     * show preferences dialog.
     */
    protected void actionPreferences() {
        this.preferencesController.showDialog();
    }

    /**
     * show game ID dialog.
     */
    protected void actionGameID() {
        this.gameidController.showDialog();
    }

    /**
     * apply updated preferences.
     */
    protected void actionUpdatedPrefs(final boolean isNewBoard, final boolean isNewSize) {
        if (isNewBoard) {
            this.gameState.setNewRandomBoard();
            this.hideHint();
        }
        this.boardController.initBoardPanel();
        if (isNewSize) {
            this.mainView.update();
        }
        this.internalUpdateBoardColors();
    }

    /**
     * repaint board using this UI color scheme.
     */
    protected void actionRepaintBoardUiColors(final Color[] uiColors, final GridLinesEnum gle, final BoardColorNumbersEnum bcne, final int numColors, final HighlightColorEnum hce) {
        this.boardController.actionRepaintBoardUiColors(uiColors, gle, bcne, hce);
        this.controlController.actionSetButtonColors(uiColors, numColors);
    }

    /**
     * select a game progress in gamestate.
     * @param numProgress number of the selected solution (0 == user solution, other = solver solutions)
     */
    protected void actionSelectGameProgress(final int numProgress) {
        final boolean isDone = this.gameState.selectGameProgress(numProgress);
        if (isDone) {
            this.internalUpdateBoardColors();
        }
    }

    /**
     * this class handles the Property Change Events coming from GameState
     * when the solver(s) running in a worker thread present their solutions.
     */
    private class GameStatePropertyChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if (GameState.PROPERTY_PROGRESS_SOLUTIONS.equals(evt.getPropertyName())) {
                final GameProgress newValue = (GameProgress) evt.getNewValue();
                if (null == newValue) {
                    MainController.this.controlController.actionClearSolverResults();
                } else {
                    MainController.this.controlController.actionAddSolverResult(newValue);
                }
            } else if (GameState.PROPERTY_HINT.equals(evt.getPropertyName())) {
                if ((null != evt.getOldValue()) && (null != evt.getNewValue())) {
                    final Integer color = (Integer)evt.getOldValue();
                    final Integer estimatedSteps = (Integer)evt.getNewValue();
                    System.out.println("hint: color=" + color + " estimatedSteps=" + estimatedSteps); System.out.println();
                    MainController.this.controlController.actionShowHint(color, estimatedSteps);
                }
            }
        }
    }

    /**
     * run the solver to calculate a hint for the user's next step.
     */
    public void actionCalculateHint() {
        if (this.gameState.isUserProgress()) {
            this.gameState.calculateHint();
        }
    }

    private void hideHint() {
        this.gameState.removeHint();
        this.controlController.actionHideHint();
    }

    /**
     * @param hintColor
     */
    public void actionHighlightColor(int hintColor) {
        this.boardController.actionHightlightFloodNeighborCells(hintColor);
    }
}
