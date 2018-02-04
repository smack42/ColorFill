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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The Board class represents the board (or game problem)
 */
public class Board {

    private final byte[] cells;
    private final int width, height;
    private final int colors;
    private final ColorArea[] cellsColorAreas;
    private final SortedSet<ColorArea> colorAreas;
    private int startPos = -1; // -1 == none
    private int depth = -1; // -1 == not yet set
    private ColorArea[] idsColorAreas;
    private byte[] idsColors;
    private int sizeColorAreas8;

    /**
     * construct a new Board using the specified parameters.
     * the cells are filled with random color valaues.
     * 
     * @param width
     * @param height
     * @param colors
     */
    public Board(final int width, final int height, final int colors) {
        this.width = width;
        this.height = height;
        final int len = width * height;
        this.colors = colors;
        this.cells = new byte[len];
        this.cellsColorAreas = new ColorArea[len];
        this.colorAreas = new TreeSet<ColorArea>();
        this.randomCellColors();
        this.startPos = this.depth = -1;
    }

    /**
     * fill all board cells with random color values.
     */
    private void randomCellColors() {
        final Random random = new Random();
        for (int i = 0;  i < this.cells.length;  ++i) {
            final byte color = (byte)random.nextInt(this.colors);
            this.cells[i] = color;
        }
        this.colorAreas.clear();
        this.colorAreas.addAll(this.createColorAreas());
    }

    /**
     * construct a new Board from a text representation:
     * each character represents the color (number 1...n) of a cell in a square grid.
     * the length of the string must therefore be a square number (like 14x14=196).
     * 
     * an example file with 1000 boards in a text file can be found here:
     * http://cplus.about.com/od/programmingchallenges/a/challenge19.htm
     * 
     * @param str board in text form, any whitespace characters will be ignored
     */
    public Board(String str) {
        str = str.replaceAll("\\s", ""); // remove whitespace
        final int len = str.length(); // TODO check if "len" is a square number
        this.cells = new byte[len];
        this.width = (int)Math.sqrt(len);
        this.height = this.width;
        for (int i = 0;  i < len;  ++i) {
            final char c = str.charAt(i);
            this.cells[i] = (byte)(Byte.parseByte(String.valueOf(c)) - 1);
        }
        this.cellsColorAreas = new ColorArea[len];
        this.colorAreas = new TreeSet<ColorArea>();
        this.colorAreas.addAll(this.createColorAreas());
        int maxColor = 0;
        for (final byte color : this.cells) {
            maxColor = Math.max(maxColor, color);
        }
        this.colors = maxColor + 1;
        this.startPos = this.depth = -1;
    }

    /**
     * construct a new Board from a text representation and set the start position.
     * @param str
     * @param startPos
     */
    public Board(final String str, final int startPos) {
        this(str);
        this.determineColorAreasDepth(startPos);
    }

    /**
     * construct a new Board from values and text representation and set the start position.
     * warning: may throw an exception if the specified parameters are inconsistent!
     *
     * @param width
     * @param height
     * @param colors
     * @param strCells
     * @param startPos
     */
    public Board(final int width, final int height, final int colors, final String strCells, final int startPos) {
        this(width, height, colors);
        for (int i = 0;  i < this.cells.length;  ++i) {
            final char c = strCells.charAt(i);
            this.cells[i] = (byte)(Byte.parseByte(String.valueOf(c)) - 1);
        }
        this.colorAreas.clear();
        this.colorAreas.addAll(this.createColorAreas());
        this.determineColorAreasDepth(startPos);
    }

    /**
     * construct a new from the values of the specified GameProgress.
     * it copies the Board and then applies the steps already done in the user's solution.
     * @param gp
     * @return
     */
    public Board(final GameProgress gp) {
        this(gp.getBoard().getWidth(), gp.getBoard().getHeight(), gp.getBoard().getNumColors());
        final int[] otherCells = gp.getColors();
        for (int i = 0;  i < this.cells.length;  ++i) {
            this.cells[i] = (byte)(otherCells[i]);
        }
        this.colorAreas.clear();
        this.colorAreas.addAll(this.createColorAreas());
        this.determineColorAreasDepth(gp.getBoard().getStartPos());
    }

    private Set<ColorArea> createColorAreas() {
        final Set<ColorArea> result = new HashSet<ColorArea>();
        // build ColorAreas, fill with them with members: adjacent cells of the same color
        for (int index = 0;  index < this.cells.length;  ++index) {
            final byte color = this.cells[index];
            boolean isAdded = false;
            for (final ColorArea ca : result) {
                if (ca.addMember(index, color)) {
                    isAdded = true;
                    break; // for()
                }
            }
            if (false == isAdded) {
                final ColorArea ca = new ColorArea(color, this.width);
                ca.addMember(index, color);
                result.add(ca);
            }
        }
        // merge ColorAreas that are neighbors of the same color
        for (boolean doMergeAreas = true;  true == doMergeAreas;  ) {
            final Set<ColorArea> mergedAreas = new HashSet<ColorArea>();
            for (final ColorArea ca1 : result) {
                if (false == mergedAreas.contains(ca1)) {
                    for (final ColorArea ca2 : result) {
                        if (false == mergedAreas.contains(ca2)) {
                            if (true == ca1.addMembers(ca2)) {
                                mergedAreas.add(ca2);
                            }
                        }
                    }
                }
            }
            result.removeAll(mergedAreas);
            doMergeAreas = (mergedAreas.size() > 0);
        }
        // connect all ColorAreas to all of their neighbors
        for (final ColorArea ca1 : result) {
            for (final ColorArea ca2 : result) {
                ca1.addNeighbor(ca2);
            }
        }
        // set cellsColorAreas and idsColorAreas
        int id = 0;
        this.idsColorAreas = new ColorArea[result.size()];
        this.idsColors = new byte[result.size()];
        this.sizeColorAreas8 = (result.size() + 7) >> 3; // how many bytes are needed to store them as bits?
        for (final ColorArea ca : result) {
            ca.setId(id++);
            this.idsColorAreas[ca.getId()] = ca;
            this.idsColors[ca.getId()] = ca.getColor();
            for (final int member : ca.getMembers()) {
                this.cellsColorAreas[member] = ca;
            }
        }
        for (final ColorArea ca : result) {
            ca.makeNeighborsArray();
        }
        return result;
    }


    /**
     * parse the text representation of the solution
     * and check if it solves this board.
     * 
     * @param str characters 1...n (color values), any whitespace characters will be ignored
     * @param startPos position of the board cell where the color flood starts (0 == top left)
     * @return error message in case the check fails, empty string if check is successful
     */
    public String checkSolution(String str, final int startPos) {
        // parse the solution text
        str = str.replaceAll("\\s", ""); // remove whitespace
        final int len = str.length();
        final byte[] solution = new byte[len];
        for (int i = 0;  i < len;  ++i) {
            final char c = str.charAt(i);
            solution[i] = (byte)(Byte.parseByte(String.valueOf(c)) - 1);
        }
        final Set<ColorArea> floodAreas = new TreeSet<ColorArea>();
        final Set<ColorArea> floodNeighbors = new TreeSet<ColorArea>();
        int floodColor = 0;
        // start with the ColorArea that contains cell startPos
        final ColorArea startCa = this.getColorArea4Cell(startPos);
        floodColor = startCa.getColor();
        floodAreas.add(startCa);
        floodNeighbors.addAll(startCa.getNeighbors());
        // apply all colors from solution
        for (final byte solutionColor : solution) {
            if (floodColor == solutionColor) {
                return "error in solution: duplicate color " + (solutionColor + 1);
            }
            floodColor = solutionColor;
            // add all floodNeighbors of matching color to floodAreas
            final Set<ColorArea> newFloodAreas = new TreeSet<ColorArea>();
            for (final ColorArea ca : floodNeighbors) {
                if (ca.getColor() == floodColor) {
                    newFloodAreas.add(ca);
                    floodAreas.add(ca);
                }
            }
            if (newFloodAreas.isEmpty()) {
                return "error in solution: useless color " + (floodColor + 1);
            }
            // remove the newly flooded areas from floodNeighbors
            floodNeighbors.removeAll(newFloodAreas);
            // add new neighbors to floodNeighbors
            for (final ColorArea ca : newFloodAreas) {
                for (final ColorArea caN : ca.getNeighborsArray()) {
                    if (false == floodAreas.contains(caN)) {
                        floodNeighbors.add(caN);
                    }
                }
            }
        }
        // solution finished, check if board is completely flooded
        if ((floodAreas.size() != this.colorAreas.size()) ||
                (false == floodAreas.containsAll(this.colorAreas)) ||
                (false == floodNeighbors.isEmpty())) {
            return "error in solution: board is not completely flooded"
                    + "\n floodAreas:     " + floodAreas
                    + "\n floodNeighbors: " + floodNeighbors
                    ;
        } else {
            return ""; // check OK
        }
    }


    /**
     * starting at startPos, follow the connected neighbors of all color areas
     * and mark them all with their depth (number of levels from startPos).
     * 
     * @param startPos position of the board cell where the color flood starts (0 == top left)
     * @return maximum depth of all color areas of this board
     */
    public synchronized int determineColorAreasDepth(final int startPos) {
        if (this.startPos == startPos) {
            return this.depth;
        }
        // init
        this.startPos = startPos;
        for (final ColorArea ca : this.colorAreas) {
            ca.setDepth(Integer.MAX_VALUE);
        }
        int depth = 0, result = 0;
        Collection<ColorArea> nextLevel = new ArrayList<ColorArea>();
        // find the ColorArea that contains cell startPos
        final ColorArea startCa = this.getColorArea4Cell(startPos);
        startCa.setDepth(depth);
        nextLevel.addAll(startCa.getNeighbors());
        // visit all ColorAreas and mark them with their depth
        while (false == nextLevel.isEmpty()) {
            ++depth;
            final Collection<ColorArea> thisLevel = nextLevel;
            nextLevel = new ArrayList<ColorArea>();
            for (final ColorArea ca : thisLevel) {
                if (ca.getDepth() > depth) {
                    ca.setDepth(depth);
                    nextLevel.addAll(ca.getNeighbors());
                    result = depth;
                }
            }
        }
        this.depth = result;
        return result;
    }


    public String toStringColorDepth(final int startPos) {
        final int maxDepth = this.determineColorAreasDepth(startPos);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0;  i < this.cellsColorAreas.length;  ++i) {
            final ColorArea ca = this.cellsColorAreas[i];
            sb.append(ca.getColor() + 1).append('_').append(ca.getDepth());
            if (10 > ca.getDepth()) {
                sb.append(' ');
            }
            if (0 == (i + 1) % width) {
                sb.append('\n');
            } else {
                sb.append(' ');
            }
        }
        sb.append("maxDepth=").append(maxDepth);
        return sb.toString();
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0;  i < this.cells.length;  ++i) {
            sb.append(this.cells[i] + 1);
            if (0 == (i + 1) % width) {
                sb.append('\n');
            }
        }
        sb.append(this.colorAreas);
        return sb.toString();
    }


    public String toStringCells() {
        final StringBuilder sb = new StringBuilder();
        for (final byte cell : this.cells) {
            sb.append(cell + 1);
        }
        return sb.toString();
    }


    public Set<ColorArea> getColorAreas() {
        return this.colorAreas;
    }

    public ColorArea getColorArea4Cell(int cell) {
        return this.cellsColorAreas[cell];
    }

    public ColorArea getColorArea4Id(int id) {
        return this.idsColorAreas[id];
    }

    public byte getColor4Id(int id) {
        return this.idsColors[id];
    }

    public ColorArea[] getColorAreasArray() {
        return this.idsColorAreas;
    }

    public int getColor(int cell) {
        return this.cells[cell];
    }

    public synchronized int getStartPos() {
        return this.startPos;
    }

    public int getDepth(int startPos) {
        return this.determineColorAreasDepth(startPos);
    }

    public int getNumColors() {
        return this.colors;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getSize() {
        return this.cells.length;
    }

    public int getSizeColorAreas8() {
        return this.sizeColorAreas8;
    }
}
