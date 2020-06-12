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
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The Board class represents the board (or game problem)
 */
public class Board {
    /** maximum number of colors supported by the most susceptible solver algorithm */
    public static final int MAX_NUMBER_OF_COLORS = 16;

    private final byte[] cells;
    private final int width, height;
    private final SortedMap<Character, Byte> char2Color;
    private final SortedMap<Byte, Character> color2Char;
    private final int colors;
    private final ColorArea[] cellsColorAreas;
    private final SortedSet<ColorArea> colorAreas;
    private int startPos = -1; // -1 == none
    private int depth = -1; // -1 == not yet set
    private ColorArea[] idsColorAreas;
    private byte[] idsColors;
    private ColorAreaSet[] idsNeighborColorAreaSets;
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
        final Random random = new Random();
        for (int i = 0;  i < this.cells.length;  ++i) {
            final byte color = (byte)random.nextInt(this.colors);
            this.cells[i] = color;
        }
        this.startPos = this.depth = -1;
        this.char2Color = new TreeMap<Character, Byte>();
        this.color2Char = new TreeMap<Byte, Character>();
        for (final byte cell : this.cells) {
            // first color 0 is character '1'
            final Character c = Character.valueOf(Character.forDigit(cell + 1, MAX_NUMBER_OF_COLORS + 1));
            final Byte b = Byte.valueOf(cell);
            this.char2Color.put(c, b);
            this.color2Char.put(b, c);
        }
        this.colorAreas = new TreeSet<ColorArea>();
        this.colorAreas.addAll(this.createColorAreas());
    }

    /**
     * fills <code>this.char2Color</code> and <code>this.color2Char</code> and <code>this.cells</code>.
     * @throws IllegalArgumentException if there are more than MAX_NUMBER_OF_COLORS distinct character values in <code>str</code>
     */
    private void importString(String str) {
        final int len = str.length();
        this.char2Color.clear();
        this.color2Char.clear();
        for (int i = 0;  i < len;  ++i) {
            this.char2Color.put(Character.valueOf(str.charAt(i)), null);
        }
        if (this.char2Color.size() > MAX_NUMBER_OF_COLORS) {
            throw new IllegalArgumentException("more than " + MAX_NUMBER_OF_COLORS + " color values found in input string: " + this.char2Color.keySet().toString() + " \"" + str + "\"");
        }
        byte b = 0;
        for (Map.Entry<Character, Byte> entry : this.char2Color.entrySet()) {
            entry.setValue(Byte.valueOf(b++));
            this.color2Char.put(entry.getValue(), entry.getKey());
        }
        for (int i = 0;  i < len;  ++i) {
            this.cells[i] = this.char2Color.get(Character.valueOf(str.charAt(i))).byteValue();
        }
    }

    public String solutionToString(byte[] steps) {
        final StringBuilder result = new StringBuilder();
        for (final byte color : steps) {
            result.append(this.color2Char.get(Byte.valueOf(color)).charValue());
        }
        return result.toString();
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
        final int len = str.length();
        this.cells = new byte[len];
        this.width = (int)Math.sqrt(len);
        this.height = this.width;
        if (this.width * this.height != len) {
            throw new IllegalArgumentException("length of input String is not a square number: " + len + " \"" + str + "\"");
        }
        this.char2Color = new TreeMap<Character, Byte>();
        this.color2Char = new TreeMap<Byte, Character>();
        this.importString(str);
        this.colors = this.char2Color.size();
        this.cellsColorAreas = new ColorArea[len];
        this.colorAreas = new TreeSet<ColorArea>();
        this.colorAreas.addAll(this.createColorAreas());
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
        this.importString(strCells);
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
        // build ColorAreas
        for (int cell = 0, row = 0;  row < this.height;  ++row) {
            for (int column = 0;  column < this.width;  ++column, ++cell) {
                final byte color = this.cells[cell];
                final ColorArea topCa  = (0 == row    ? null : this.cellsColorAreas[cell - this.width]);
                final ColorArea leftCa = (0 == column ? null : this.cellsColorAreas[cell - 1]);
                final ColorArea cellCa;
                if ((topCa != null) && (topCa.getColor() == color) && (leftCa != null) && (leftCa.getColor() == color)) {
                    if (topCa != leftCa) { // not same object - merge leftCa into topCa
                        for (Integer member : leftCa.getMembers()) {
                            topCa.addMember(member.intValue());
                            this.cellsColorAreas[member.intValue()] = topCa;
                        }
                        result.remove(leftCa);
                    }
                    cellCa = topCa;
                } else if ((topCa != null) && (topCa.getColor() == color)) {
                    cellCa = topCa;
                } else if ((leftCa != null) && (leftCa.getColor() == color)) {
                    cellCa = leftCa;
                } else {
                    cellCa = new ColorArea(color, this.color2Char.get(Byte.valueOf(color)));
                    result.add(cellCa);
                }
                cellCa.addMember(cell);
                this.cellsColorAreas[cell] = cellCa;
            }
        }
        // connect neighbor ColorAreas
        for (int cell = 0, row = 0;  row < this.height;  ++row) {
            for (int column = 0;  column < this.width;  ++column, ++cell) {
                final ColorArea cellCa = this.cellsColorAreas[cell];
                if (row > 0) {
                    cellCa.connectNeighbor(this.cellsColorAreas[cell - this.width]); // top
                }
                if (column > 0) {
                    cellCa.connectNeighbor(this.cellsColorAreas[cell - 1]); // left
                }
            }
        }
        // set ID's of ColorAreas and prepare some lookup arrays
        int id = 0;
        this.idsColorAreas = new ColorArea[result.size()];
        this.idsColors = new byte[result.size()];
        this.sizeColorAreas8 = (result.size() + 7) >> 3; // how many bytes are needed to store them as bits?
        for (final ColorArea ca : result) {
            ca.setId(id++);
            this.idsColorAreas[ca.getId()] = ca;
            this.idsColors[ca.getId()] = ca.getColor();
        }
        for (final ColorArea ca : result) {
            ca.makeNeighborsArray(this);
        }
        this.idsNeighborColorAreaSets = new ColorAreaSet[result.size()];
        for (final ColorArea ca : result) {
            this.idsNeighborColorAreaSets[ca.getId()] = ca.getNeighborsColorAreaSet();
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
            solution[i] = this.char2Color.get(Character.valueOf(c)).byteValue();
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
            sb.append(ca.getColorChar()).append('_').append(ca.getDepth());
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
            sb.append(this.color2Char.get(Byte.valueOf(this.cells[i])).charValue());
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
            sb.append(this.color2Char.get(Byte.valueOf(cell)).charValue());
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

    public ColorAreaSet getNeighborColorAreaSet4Id(int id) {
        return this.idsNeighborColorAreaSets[id];
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
