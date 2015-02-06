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

public enum GridLinesEnum {

    NONE   (0, "pref.gridLines.none.txt"),
    ALL    (1, "pref.gridLines.all.txt"),
    COLORS (2, "pref.gridLines.colors.txt");

    public final int intValue;
    public final String l10nKey;

    private GridLinesEnum(final int intValue, final String l10nKey) {
        this.intValue = intValue;
        this.l10nKey = l10nKey; //L10N = Localization
    }

    /**
     * get the GridLinesEnum for the specified intValue,
     * or null if none was found.
     * @param intValue
     * @return
     */
    public static GridLinesEnum valueOf(final int intValue) {
        GridLinesEnum result = null;
        for (final GridLinesEnum gle : values()) {
            if (gle.intValue == intValue) {
                result = gle;
                break;
            }
        }
        return result;
    }
}
