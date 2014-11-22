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

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JPanel;

import colorfill.model.GameState;

/**
 * this controller handles the control flows of BoardPanel.
 */
public class BoardController {

    private final MainController mainController;
    private final GameState gameState;
    private final BoardPanel boardPanel;

    protected BoardController(final MainController mainController, final GameState gameState) {
        this.mainController = mainController;
        this.gameState = gameState;
        this.boardPanel = new BoardPanel(this);
        this.initBoardPanel();
    }

    /**
     * apply current values of board width + height and color schmeme
     */
    protected void initBoardPanel() {
        this.boardPanel.init(this.gameState.getBoard().getWidth(), this.gameState.getBoard().getHeight(), this.gameState.getPreferences().getUiColors());
        this.repaintBoardPanel();
    }

    private void repaintBoardPanel() {
        this.boardPanel.setCellColors(this.gameState.getSelectedProgress().getColors());
    }

    protected JPanel getPanel() {
        return this.boardPanel;
    }

    protected Color[] getUiColors() {
        return this.gameState.getPreferences().getUiColors();
    }

    /**
     * called by BoardPanel when user clicks a mouse button on a board cell.
     * @param e MouseEvent
     * @param index the cell index
     * @param color the cell color
     */
    protected void userClickedOnCell(final MouseEvent e, final int index, final int color) {
        if (this.gameState.isUserProgress() && this.gameState.getSelectedProgress().isFloodNeighborCell(index)) {
            this.mainController.actionAddStep(color);
        }
    }

    /**
     * called when the colors of the board cells should be updated from the model.
     */
    protected void actionUpdateBoardColors() {
        this.repaintBoardPanel();
    }

    /**
     * called by BoardPanel when user moves the mouse pointer to a board cell.
     * @param e MouseEvent
     * @param index the cell index
     * @param color the cell color
     */
    protected void userMovedMouseToCell(MouseEvent e, int index, int color) {
        if (this.gameState.isUserProgress()) {
            final Collection<Integer> neighborCells;
            if (this.gameState.getSelectedProgress().isFloodNeighborCell(index)) {
                neighborCells = this.gameState.getSelectedProgress().getFloodNeighborCells(color);
            } else {
                neighborCells = Collections.emptyList();
            }
            this.boardPanel.highlightCells(neighborCells);
        }
    }

    /**
     * highlight the flodd neighbor cells of the specified color.
     * @param color
     */
    protected void actionHightlightFloodNeighborCells(final int color) {
        final Collection<Integer> neighborCells = this.gameState.getSelectedProgress().getFloodNeighborCells(color);
        this.boardPanel.highlightCells(neighborCells);
    }
}
