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

import javax.swing.SwingUtilities;

import colorfill.model.BoardColorNumbersEnum;
import colorfill.model.GamePreferences;
import colorfill.model.GameState;
import colorfill.model.GridLinesEnum;
import colorfill.model.HighlightColorEnum;
import colorfill.model.StartPositionEnum;

public class PreferencesController {

    private final MainController mainController;
    private final GameState gameState;
    private final PreferencesDialog prefDialog;

    protected PreferencesController(final MainController mainController, final GameState gameState, final MainWindow mainWindow,
            final String progname, final String version, final String author) {
        this.mainController = mainController;
        this.gameState = gameState;
        this.prefDialog = new PreferencesDialog(this, mainWindow, progname, version, author, this.getAllUiColors());
    }

    protected void showDialog() {
        this.prefDialog.showDialog();
    }

    /**
     * called by PreferencesDialog when user pressed the "OK" button.
     */
    protected void userPrefsOK(
            final int width,
            final int height,
            final int numColors,
            final StartPositionEnum spe,
            final GridLinesEnum gle,
            final BoardColorNumbersEnum bcne,
            final int uiColorsNumber,
            final int cellSize,
            final HighlightColorEnum hce,
            final String lafName ) {
        boolean isNewBoard = this.gameState.getPreferences().setWidth(width);
        isNewBoard |= this.gameState.getPreferences().setHeight(height);
        boolean isNewSize = isNewBoard;
        isNewBoard |= this.gameState.getPreferences().setNumColors(numColors);
        isNewBoard |= this.gameState.getPreferences().setStartPos(spe);
        isNewSize |= this.gameState.getPreferences().setCellSize(cellSize);
        this.gameState.getPreferences().setGridLines(gle);
        this.gameState.getPreferences().setBoardColorNumbers(bcne);
        this.gameState.getPreferences().setUiColorsNumber(uiColorsNumber);
        this.gameState.getPreferences().setHighlightColor(hce);
        boolean isNewLaf = this.gameState.getPreferences().setLafName(lafName);
        this.gameState.getPreferences().savePrefs();
        this.mainController.actionUpdatedPrefs(isNewBoard, isNewSize, isNewLaf);
    }

    /**
     * called by PreferencesDialog when user pressed the "Restore Defaults" button.
     */
    protected void userDefaults() {
        this.prefDialog.setValues(
                GamePreferences.DEFAULT_BOARD_WIDTH,
                GamePreferences.DEFAULT_BOARD_HEIGHT,
                GamePreferences.DEFAULT_BOARD_NUM_COLORS,
                GamePreferences.DEFAULT_BOARD_STARTPOS,
                GamePreferences.DEFAULT_UI_GRIDLINES,
                GamePreferences.DEFAULT_UI_BOARD_COLOR_NUMBERS,
                GamePreferences.DEFAULT_UI_HIGHLIGHT_COLOR,
                GamePreferences.DEFAULT_UI_COLSCHEME,
                GamePreferences.DEFAULT_UI_CELLSIZE,
                GamePreferences.DEFAULT_UI_LAFNAME );
    }

    /**
     * called by PreferencesDialog when user selects a color scheme or grid line option.
     */
    protected void userPreviewUiColors(final int colorSchemeNumber, final GridLinesEnum gle, final BoardColorNumbersEnum bcne, final int numColors, final HighlightColorEnum hce) {
        this.mainController.actionRepaintBoardUiColors(this.getAllUiColors()[colorSchemeNumber], gle, bcne, numColors, hce);
    }

    protected int getWidth() {
        return this.gameState.getPreferences().getWidth();
    }
    protected int getHeight() {
        return this.gameState.getPreferences().getHeight();
    }
    protected int getNumColors() {
        return this.gameState.getPreferences().getNumColors();
    }
    protected Color[][] getAllUiColors() {
        return this.gameState.getPreferences().getAllUiColors();
    }
    protected int getUiColorsNumber() {
        return this.gameState.getPreferences().getUiColorsNumber();
    }
    protected StartPositionEnum getStartPos() {
        return this.gameState.getPreferences().getStartPosEnum();
    }
    protected GridLinesEnum getGridLines() {
        return this.gameState.getPreferences().getGridLinesEnum();
    }
    protected BoardColorNumbersEnum getBoardColorNumbers() {
        return this.gameState.getPreferences().getBoardColorNumbersEnum();
    }
    protected HighlightColorEnum getHighlightColor() {
        return this.gameState.getPreferences().getHighlightColorEnum();
    }
    protected int getCellSize() {
        return this.gameState.getPreferences().getCellSize();
    }
    protected String getLafName() {
        return this.gameState.getPreferences().getLafName();
    }
}
