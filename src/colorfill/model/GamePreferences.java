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

package colorfill.model;

import java.awt.Color;

public class GamePreferences {

    private static final int DEFAULT_BOARD_WIDTH  = 14;
    private static final int DEFAULT_BOARD_HEIGHT = 14;
    private static final int DEFAULT_BOARD_NUM_COLORS = 6;
    private static final int DEFAULT_BOARD_STARTPOS = 0; // 0 == top left corner
    private static final Color[][] DEFAULT_UI_COLORS = {
        { // Flood-It scheme
            new Color(0xDC4A20), // Color.RED
            new Color(0x7E9D1E), // Color.GREEN
            new Color(0x605CA8), // Color.BLUE
            new Color(0xF3F61D), // Color.YELLOW
            new Color(0x46B1E2), // Color.CYAN
            new Color(0xED70A1)  // Color.MAGENTA
        },
        { // Color Flood (Android) scheme 1 (default)
            new Color(0x6261A8),
            new Color(0x6AAECC),
            new Color(0x5EDD67),
            new Color(0xF66A61),
            new Color(0xF6BF61),
            new Color(0xF0F461)
        },
        { // Color Flood (Android) scheme 6
            new Color(0xDF5162),
            new Color(0x38322F),
            new Color(0x247E86),
            new Color(0x1BC4C1),
            new Color(0xFCF8C9),
            new Color(0xD19C2D)
        }
    };

    private int width;
    private int height;
    private int numColors;
    private int startPos;
    private int uiColors;

    public GamePreferences() {
        this.width = DEFAULT_BOARD_WIDTH;
        this.height = DEFAULT_BOARD_HEIGHT;
        this.numColors = DEFAULT_BOARD_NUM_COLORS;
        this.startPos = DEFAULT_BOARD_STARTPOS;
        this.uiColors = 0;
    }

    public int getWidth() {
        return this.width;
    }
    public boolean setWidth(final int width) {
        if (this.width != width) { // TODO setWidth validation
            this.width = width;
            return true; // new value has been set
        }
        return false; // value not changed
    }

    public int getHeight() {
        return this.height;
    }
    public boolean setHeight(final int height) {
        if (this.height != height) { // TODO setHeight validation
            this.height = height;
            return true; // new value has been set
        }
        return false; // value not changed
    }

    public int getNumColors() {
        return this.numColors;
    }
    public void setNumColors(int numColors) {
        this.numColors = numColors;
    }

    public int getStartPos() {
        return this.startPos;
    }
    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public Color[][] getAllUiColors() {
        return DEFAULT_UI_COLORS;
    }
    public Color[] getUiColors() {
        return DEFAULT_UI_COLORS[this.uiColors];
    }
    public int getUiColorsNumber() {
        return this.uiColors;
    }
    public void setUiColorsNumber(final int num) {
        if ((num >= 0) && (num < DEFAULT_UI_COLORS.length)) {
            this.uiColors = num;
        }
    }
}
