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

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * ColorArea represents a connected area of cells that have the same color.
 */
public class ColorArea implements Comparable<ColorArea> {
    private int id;
    private final byte color;
    private final char colorChar;
    private final int boardWidth;
    private final SortedSet<Integer> members = new TreeSet<Integer>(); // sorted set - used by compareTo!
    private final SortedSet<Integer> membersUnmodifiable = Collections.unmodifiableSortedSet(this.members);
    private final SortedSet<ColorArea> neighbors = new TreeSet<ColorArea>();
    private final SortedSet<ColorArea> neighborsUnmodifiable = Collections.unmodifiableSortedSet(this.neighbors);
    private ColorArea[]  neighborsArray = null; // will be created by makeNeighborsArray()
    private ColorAreaSet neighborsCaSet = null; // will be created by makeNeighborsArray()
    private int depth = 0;

    /**
     * used by the AStar (A*) search algorithm to erase / re-calculate "dynamic depth" info
     */
    public int tmpAStarDepth;

    protected ColorArea(final byte color, final Character colorChar, final int boardWidth) {
        this.color = color;
        this.colorChar = colorChar.charValue();
        this.boardWidth = boardWidth;
    }

    private boolean isNeighborCell(final int index) {
        for (final int member : this.members) {
            if ((((index == member - 1) || (index == member + 1))
                    && (index / this.boardWidth == member / this.boardWidth)) ||
                (index == member - this.boardWidth) ||
                (index == member + this.boardWidth)) {
                return true;
            }
        }
        return this.members.isEmpty();
    }

    private boolean isNeighborArea(final ColorArea other) {
        for (final int otherMember : other.members) {
            if (this.isNeighborCell(otherMember)) {
                return true;
            }
        }
        return other.members.isEmpty();
    }

    boolean addMember(final int index, final byte color) {
        if (this.color != color) {
            return false; // wrong (different) color
        }
        if (this.isNeighborCell(index)) {
            return this.members.add(Integer.valueOf(index)); // added
        }
        return false; // not added
    }

    boolean addMembers(final ColorArea other) {
        if (this.color != other.color) {
            return false; // wrong (different) color
        }
        if (this.isNeighborArea(other) && (false == other.members.containsAll(this.members))) {
            return this.members.addAll(other.members); // added
        }
        return false; // not added
    }

    boolean addNeighbor(final ColorArea other) {
        if (this.color == other.color) {
            return false; // wrong (same) color
        }
        if (this.isNeighborArea(other)) {
            return this.neighbors.add(other); // added
        }
        return false; // not added
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.colorChar).append('_').append(this.members.toString()).append("-(");
        for (final ColorArea ca : this.neighbors) {
            sb.append(ca.colorChar);
        }
        sb.append(')');
        return sb.toString();
    }

    // sorted by color, number of members, first (smallest) member
    @Override
    public int compareTo(final ColorArea other) {
        if (this.color < other.color) {
            return -1;
        } else if (this.color > other.color) {
            return 1;
        } else { // equal color
            if (this.members.size() < other.members.size()) {
                return -1;
            } else if (this.members.size() > other.members.size()) {
                return 1;
            } else { // equal number of members
                if (this.members.isEmpty()) {
                    return 0; // no members
                } else {
                    final int thisMember = this.members.iterator().next().intValue();
                    final int otherMember = other.members.iterator().next().intValue();
                    if (thisMember < otherMember) {
                        return -1;
                    } else if (thisMember > otherMember) {
                        return 1;
                    } else {
                        return 0; // equal first member
                    }
                }
            }
        }
    }

    public byte getColor() {
        return this.color;
    }

    public char getColorChar() {
        return this.colorChar;
    }

    public SortedSet<Integer> getMembers() {
        return this.membersUnmodifiable;
    }

    public int getMemberSize() {
        return this.members.size();
    }

    public boolean containsMember(int i) {
        return this.members.contains(Integer.valueOf(i));
    }

    public SortedSet<ColorArea> getNeighbors() {
        return this.neighborsUnmodifiable;
    }

    protected void makeNeighborsArray(final Board board) {
        this.neighborsArray = this.neighbors.toArray(new ColorArea[0]);
        this.neighborsCaSet = new ColorAreaSet(board);
        for (final ColorArea ca : this.neighborsArray) {
            this.neighborsCaSet.add(ca);
        }
        this.neighborsCaSet.size(); // compute internal size
    }

    public ColorArea[] getNeighborsArray() {
        return this.neighborsArray;
    }

    public ColorAreaSet getNeighborsColorAreaSet() {
        return this.neighborsCaSet;
    }

    public int getDepth() {
        return this.depth;
    }
    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getId() {
        return this.id;
    }
    protected void setId(final int id) {
        this.id = id;
    }
}
