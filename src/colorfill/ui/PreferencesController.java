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

import colorfill.model.GameState;

public class PreferencesController {

    private final MainController mainController;
    private final GameState gameState;
    private final PreferencesDialog prefDialog;

    protected PreferencesController(final MainController mainController, final GameState gameState, final MainWindow mainWindow) {
        this.mainController = mainController;
        this.gameState = gameState;
        this.prefDialog = new PreferencesDialog(this, mainWindow);
    }

    protected void showDialog() {
        this.prefDialog.showDialog();
    }

    /**
     * called by PreferencesDialog when user pressed the "OK" button.
     */
    protected void userPrefsOK() {
        this.mainController.actionUpdatedPrefs();
    }

    protected int getWidth() {
        return this.gameState.getPreferences().getWidth();
    }
    protected boolean setWidth(final int width) {
        return this.gameState.getPreferences().setWidth(width);
    }

    protected int getHeight() {
        return this.gameState.getPreferences().getHeight();
    }
    protected boolean setHeight(final int height) {
        return this.gameState.getPreferences().setHeight(height);
    }

    protected Color[][] getAllUiColors() {
        return this.gameState.getPreferences().getAllUiColors();
    }
    protected int getUiColorsNumber() {
        return this.gameState.getPreferences().getUiColorsNumber();
    }
    protected void setUiColorsNumber(final int num) {
        this.gameState.getPreferences().setUiColorsNumber(num);
    }
}
