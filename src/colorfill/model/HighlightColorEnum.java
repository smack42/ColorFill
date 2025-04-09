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

package colorfill.model;

import java.awt.Color;

public enum HighlightColorEnum {

    WHITE (Color.WHITE),
    BLACK (Color.BLACK),
    BLUE  (new Color(0x4B6EAF));

    public final Color color;

    private HighlightColorEnum(final Color color) {
        this.color = color;
    }

    /**
     * get the HighlightColorEnum for the specified intValue,
     * or null if none was found.
     * @param intValue
     * @return
     */
    public static HighlightColorEnum valueOf(final int intValue) {
        HighlightColorEnum result = null;
        for (final HighlightColorEnum hce : values()) {
            if (hce.ordinal() == intValue) {
                result = hce;
                break;
            }
        }
        return result;
    }
}
