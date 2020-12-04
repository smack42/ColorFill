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

package colorfill.model;

import java.awt.Color;

public enum HighlightColorEnum {

    WHITE (0, "pref.highlightColor.white.txt", Color.WHITE),
    BLACK (1, "pref.highlightColor.black.txt", Color.BLACK),
    BLUE  (2, "pref.highlightColor.blue.txt",  new Color(0x4B6EAF));

    public final int intValue;
    public final String l10nKey;
    public final Color color;

    private HighlightColorEnum(final int intValue, final String l10nKey, final Color color) {
        this.intValue = intValue;
        this.l10nKey = l10nKey; //L10N = Localization
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
            if (hce.intValue == intValue) {
                result = hce;
                break;
            }
        }
        return result;
    }
}
