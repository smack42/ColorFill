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

public enum StartPositionEnum {

    TOP_LEFT    (-1, "pref.startPos.topLeft.txt"),
    TOP_RIGHT   (-2, "pref.startPos.topRight.txt"),
    BOTTOM_LEFT (-3, "pref.startPos.bottomLeft.txt"),
    BOTTOM_RIGHT(-4, "pref.startPos.bottomRight.txt"),
    CENTRAL     (-5, "pref.startPos.central.txt");

    public final int intValue;
    public final String l10nKey;

    private StartPositionEnum(final int intValue, final String l10nKey) {
        this.intValue = intValue;
        this.l10nKey = l10nKey; //L10N = Localization
    }

    /**
     * get the StartPositionEnum for the specified intValue,
     * or null if none was found.
     * @param intValue
     * @return
     */
    public static StartPositionEnum valueOf(final int intValue) {
        StartPositionEnum result = null;
        for (final StartPositionEnum spe : values()) {
            if (spe.intValue == intValue) {
                result = spe;
                break;
            }
        }
        return result;
    }

    /**
     * calculate the start position for this StartPositionEnum.
     * @param width
     * @param height
     * @return
     */
    public int calculatePosition(final int width, final int height) {
        switch (this) {
        case TOP_LEFT:
            return 0;
        case TOP_RIGHT:
            return width - 1;
        case BOTTOM_LEFT:
            return width * (height - 1);
        case BOTTOM_RIGHT:
            return width * height - 1;
        case CENTRAL:
            return (height / 2) * width + width / 2;
        default:
            throw new IllegalArgumentException("missing implementation of StartPositionEnum.calculatePosition() for " + this.name());
        }
    }

    /**
     * calculate the start position for a certain startPos, which may
     * be a special value (StartPositionEnum.intValue)
     * or a regular number 0...width*height-1.
     * @param startPos
     * @param width
     * @param height
     * @return
     */
    public static int calculatePosition(final int startPos, final int width, final int height) {
        final StartPositionEnum spe = StartPositionEnum.valueOf(startPos);
        if (null != spe) {
            return spe.calculatePosition(width, height);
        } else {
            return startPos;
        }
    }

    /**
     * determine the intValue of the StartPositionEnum value
     * that matches the specified position (with width and height)
     * or the position value if no matching StartPositionEnum exists.
     * @param position
     * @param width
     * @param height
     * @return
     */
    public static int intValueFromPosition(final int position, final int width, final int height) {
        for (final StartPositionEnum spe : values()) {
            if (position == spe.calculatePosition(width, height)) {
                return spe.intValue; // found the enum value
            }
        }
        return position; // no enum value exists
    }
}
