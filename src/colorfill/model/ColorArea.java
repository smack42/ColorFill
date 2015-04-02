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

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.ints.IntSortedSets;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;

import java.util.Collections;
import java.util.SortedSet;

/**
 * ColorArea represents a connected area of cells that have the same color.
 */
public class ColorArea implements Comparable<ColorArea> {
    private int id;
    private final byte color;
    private final int boardWidth;
    private final IntSortedSet members = new IntAVLTreeSet(); // sorted set - used by compareTo!
    private final IntSortedSet membersUnmodifiable = IntSortedSets.unmodifiable(this.members);
    private final SortedSet<ColorArea> neighbors = new ObjectAVLTreeSet<ColorArea>();
    private final SortedSet<ColorArea> neighborsUnmodifiable = Collections.unmodifiableSortedSet(this.neighbors);
    private int depth = 0;

    protected ColorArea(final byte color, final int boardWidth) {
        this.color = (byte)color;
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
            return this.members.add(index); // added
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
        sb.append(this.color + 1).append('_').append(this.members.toString()).append("-(");
        for (final ColorArea ca : this.neighbors) {
            sb.append(ca.color + 1);
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

    public IntSortedSet getMembers() {
        return this.membersUnmodifiable;
    }

    public SortedSet<ColorArea> getNeighbors() {
        return this.neighborsUnmodifiable;
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
    public void setId(final int id) {
        this.id = id;
    }
}
