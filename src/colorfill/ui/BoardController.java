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

import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import colorfill.model.GameState;

/**
 * this controller handles the control flows of BoardPanel.
 */
public class BoardController {

    private MainController mainController;
    private GameState gameState;
    private BoardPanel boardPanel;

    protected BoardController(final MainController mainController, final GameState gameState) {
        this.mainController = mainController;
        this.gameState = gameState;
        this.boardPanel = new BoardPanel(this);
        this.initBoardPanel();
    }

    private void initBoardPanel() {
        this.boardPanel.init(this.gameState.getBoard().getWidth(), this.gameState.getBoard().getHeight());
        this.repaintBoardPanel();
    }

    private void repaintBoardPanel() {
        this.boardPanel.setCellColors(this.gameState.getColors());
    }

    protected JPanel getPanel() {
        return this.boardPanel;
    }

    /**
     * called by BoardPanel when user clicks a mouse button on a board cell.
     * @param e MouseEvent
     * @param index the cell index
     * @param color the cell color
     */
    protected void userClickedOnCell(final MouseEvent e, final int index, final int color) {
        this.mainController.actionAddStep(color);
    }

    /**
     * called when the colors of the board cells should be updated from the model.
     */
    protected void actionUpdateBoardColors() {
        this.repaintBoardPanel();
    }
}
