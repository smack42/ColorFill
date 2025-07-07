/*  ColorFill game and solver
    Copyright (C) 2014 - 2025 Michael Henke

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
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Collections;
import java.util.Map;

import javax.swing.JPanel;

import colorfill.model.BoardColorNumbersEnum;
import colorfill.model.GameState;
import colorfill.model.GridLinesEnum;
import colorfill.model.HighlightColorEnum;

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
        this.boardPanel.init(
                this.gameState.getBoard().getWidth(),
                this.gameState.getBoard().getHeight(),
                this.gameState.getPreferences().getUiColors(),
                this.gameState.getStartPos(),
                this.gameState.getPreferences().getCellSize());
        this.repaintBoardPanel();
    }

    private void repaintBoardPanel() {
        final BoardColorNumbersEnum bcne = this.gameState.getPreferences().getBoardColorNumbersEnum();
        this.boardPanel.setCellColors(this.gameState.getSelectedProgress().getColors());
        this.boardPanel.applyColorScheme(
                this.gameState.getPreferences().getUiColors(),
                this.gameState.getPreferences().getGridLinesEnum(),
                this.gameState.getSelectedProgress().getBoardColorNumbers(bcne),
                this.gameState.getPreferences().getHighlightColorEnum(),
                this.gameState.getPreferences().getHighlightTransparency() );
    }

    protected JPanel getPanel() {
        // GridBagLayout calls BoardPanel.getPreferredSize() and centers it vertically and horizontally
        final JPanel container = new JPanel(new GridBagLayout());
        container.add(this.boardPanel);
        return container;
    }

    protected Color[] getUiColors() {
        return this.gameState.getPreferences().getUiColors();
    }

    /**
     * called by BoardPanel when user clicks a mouse button on a board cell.
     * @param index the cell index
     * @param color the cell color
     */
    protected void userClickedOnCell(final int index, final int color) {
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
     * repaint board using this UI color scheme.
     */
    protected void actionRepaintBoardUiColors(
            final Color[] uiColors,
            final GridLinesEnum gle,
            final BoardColorNumbersEnum bcne,
            final HighlightColorEnum hce,
            final int highlightTransparency ) {
        this.boardPanel.applyColorScheme(uiColors, gle, this.gameState.getSelectedProgress().getBoardColorNumbers(bcne), hce, highlightTransparency);
    }

    /**
     * called by BoardPanel when user moves the mouse pointer to a board cell.
     * @param index the cell index
     * @param color the cell color
     */
    protected void userMovedMouseToCell(int index, int color) {
        if (this.gameState.isUserProgress()) {
            this.actionHightlightFloodNeighborCells(this.gameState.getSelectedProgress().isFloodNeighborCell(index)  ?  color  :  -1);
        }
    }

    /**
     * highlight the flodd neighbor cells of the specified color.
     * @param color
     */
    protected void actionHightlightFloodNeighborCells(final int color) {
        final Map<Integer, Byte> neighborCells;
        final boolean isCompleted, isDeferrable;
        if (color < 0) {
            neighborCells = Collections.emptyMap();
            isCompleted = false;
            isDeferrable = false;
        } else {
            neighborCells = this.gameState.getSelectedProgress().getFloodNeighborCellsHighlight(color);
            isCompleted = !neighborCells.isEmpty() && this.gameState.getSelectedProgress().isFloodNeighborCellsCompleted(color);
            isDeferrable = !neighborCells.isEmpty() && !isCompleted && this.gameState.getSelectedProgress().isFloodNeighborCellsDeferrable(color);
        }
        this.boardPanel.highlightCells(neighborCells, isCompleted, isDeferrable);
    }

    /** store new cellSize in game Preferences **/
    protected void userResizedWindow(final Dimension newPreferredSize) {
        if (this.gameState.getPreferences().setCellSize(newPreferredSize)) {
            this.gameState.getPreferences().savePrefs();
        }
    }
}
