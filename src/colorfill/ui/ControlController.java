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

import javax.swing.JPanel;

import colorfill.model.GameProgress;
import colorfill.model.GameState;

/**
 * this controller handles the control flows of ControlPanel.
 */
public class ControlController {

    private final MainController mainController;
    private final GameState gameState;
    private final ControlPanel controlPanel;

    protected ControlController(final MainController mainController, final GameState gameState) {
        this.mainController = mainController;
        this.gameState = gameState;
        this.controlPanel = new ControlPanel(this, gameState.getPreferences().getUiColors(), gameState.getPreferences().getNumColors(), gameState.getSolverNames());
    }

    protected JPanel getPanel() {
        return this.controlPanel;
    }

    /**
     * called when the colors of the board cells have changed and the
     * controls should be updated using values from the model.
     */
    protected void actionUpdateBoardColors() {
        final GameProgress progress = this.gameState.getSelectedProgress();
        this.controlPanel.setLabelMove(progress.getCurrentStep(), progress.isFinished());
        this.controlPanel.setButtons(progress.canUndoStep(), progress.canRedoStep(), progress.isFinished());
        this.controlPanel.setButtonColors(this.gameState.getPreferences().getUiColors(), this.gameState.getPreferences().getNumColors());
    }

    /**
     * called when the colors and number of the buttons changed. (preferences preview)
     * @param uiColors
     * @param numColors
     */
    protected void actionSetButtonColors(final Color[] uiColors, final int numColors) {
        this.controlPanel.setButtonColors(uiColors, numColors);
    }

    /**
     * called by ControlPanel when user clicks on button "New"
     */
    protected void userButtonNew() {
        this.mainController.actionNewBoard();
    }

    /**
     * called by ControlPanel when user clicks on button "Settings"
     */
    protected void userButtonPrefs() {
        this.mainController.actionPreferences();
    }

    /**
     * called by ControlPanel when user clicks on button "Undo"
     */
    protected void userButtonUndo() {
        this.mainController.actionUndoStep();
    }

    /**
     * called by ControlPanel when user clicks on button "Redo"
     */
    protected void userButtonRedo() {
        this.mainController.actionRedoStep();
    }

    /**
     * called by ControlPanel when user clicks on button "Hint"
     */
    public void userButtonHint() {
        this.mainController.actionCalculateHint();
    }

    /**
     * called by ControlPanel when mouse pointer enters or leaves "Color Hint" button.
     * @param hintColor
     */
    public void userHintColor(int hintColor) {
       this.mainController.actionHighlightColor(hintColor);
    }

    /**
     * called by ControlPanel when user clicks on one of the solution RadioButtons.
     * @param numSolution number of the selected solution (0 == user solution, other = solver solutions)
     */
    protected void userButtonSolution(final int numSolution) {
        this.mainController.actionSelectGameProgress(numSolution);
    }

    /**
     * called by ControlPanel when user clicks on a color button.
     * @param color number
     */
    protected void userButtonColor(final int color) {
        if (this.gameState.isUserProgress()) {
            this.mainController.actionAddStep(color);
        }
    }

    /**
     * remove all solver results from control panel.
     */
    protected void actionClearSolverResults() {
//        System.out.println("ControlController.actionClearSolverResults");
        this.controlPanel.clearSolverResults();
    }

    /**
     * add this solver result to control panel.
     * @param gameProgress solver result
     */
    protected void actionAddSolverResult(final GameProgress gameProgress) {
//        System.out.println("ControlController.actionAddSolverResult " + gameProgress);
        this.controlPanel.addSolverResult(gameProgress.getTotalSteps(), gameProgress.getName());
    }

    /**
     * show the hint in control panel.
     * @param color
     * @param estimatedSteps
     */
    public void actionShowHint(final Integer color, final Integer estimatedSteps) {
        this.controlPanel.showHint(color, estimatedSteps);
    }

    /**
     * hide the hint in control panel.
     */
    public void actionHideHint() {
        this.controlPanel.hideHint();
    }
}
