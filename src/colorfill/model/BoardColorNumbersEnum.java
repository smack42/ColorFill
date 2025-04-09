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

public enum BoardColorNumbersEnum {

    NONE,   // 0
    ALL,    // 1
    NEXT;   // 2

    /**
     * get the BoardColorNumbersEnum for the specified intValue,
     * or null if none was found.
     * @param intValue
     * @return
     */
    public static BoardColorNumbersEnum valueOf(final int intValue) {
        BoardColorNumbersEnum result = null;
        for (final BoardColorNumbersEnum bcne : values()) {
            if (bcne.ordinal() == intValue) {
                result = bcne;
                break;
            }
        }
        return result;
    }
}
