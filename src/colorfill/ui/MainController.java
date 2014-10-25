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

import javax.swing.SwingUtilities;

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
        this.gameState = new GameState();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(windowTitle);
            }
        });
    }

    private void createAndShowGUI(final String windowTitle) {
        this.boardController = new BoardController(this, this.gameState);
        this.controlController = new ControlController(this, this.gameState);
        this.mainView = new MainWindow(windowTitle, this.boardController.getPanel(), this.controlController.getPanel());
        this.mainView.update();
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
}
