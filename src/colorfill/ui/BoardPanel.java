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

package colorfill.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * representation of the board with its colored cells.
 */
public class BoardPanel extends JPanel {

    private static final long serialVersionUID = 8760536779314645208L;

    public static final int DEFAULT_UI_BOARD_CELL_WIDTH  = 24;
    public static final int DEFAULT_UI_BOARD_CELL_HEIGHT = 24;

    private static final Color[] COLORS = {
        // Flood-It scheme
        new Color(0xDC4A20), // Color.RED
        new Color(0x7E9D1E), // Color.GREEN
        new Color(0x605CA8), // Color.BLUE
        new Color(0xF3F61D), // Color.YELLOW
        new Color(0x46B1E2), // Color.CYAN
        new Color(0xED70A1)  // Color.MAGENTA

        // Color Flood (Android) scheme 1 (default)
//        new Color(0x6261A8),
//        new Color(0x6AAECC),
//        new Color(0x5EDD67),
//        new Color(0xF66A61),
//        new Color(0xF6BF61),
//        new Color(0xF0F461)

        // Color Flood (Android) scheme 6
//        new Color(0xDF5162),
//        new Color(0x38322F),
//        new Color(0x247E86),
//        new Color(0x1BC4C1),
//        new Color(0xFCF8C9),
//        new Color(0xD19C2D)
    };

    private final BoardController controller;
    private int columns, rows;
    private int[] cellColors = new int[0];
    private boolean[] cellHighlights = new boolean[0];

    /**
     * constructor
     * @param controller
     */
    protected BoardPanel(final BoardController controller) {
        super(true); // isDoubleBuffered
        this.controller = controller;
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                final int index = calculateCellIndex(e.getPoint());
                final int color = BoardPanel.this.cellColors[index];
                BoardPanel.this.controller.userClickedOnCell(e, index, color);
            }
        });
        this.addMouseMotionListener(new MouseMotionAdapter() {
            int currentIndex = -1;
            @Override
            public void mouseMoved(MouseEvent e) {
                final int index = calculateCellIndex(e.getPoint());
                if (this.currentIndex != index) {
                    this.currentIndex = index;
                    final int color = BoardPanel.this.cellColors[index];
                    BoardPanel.this.controller.userMovedMouseToCell(e, index, color);
                }
            }
        });
    }

    private int calculateCellIndex(final Point point) {
        final Dimension size = this.getSize();
        final int cellWidth = size.width / this.columns;
        final int cellHeight = size.height / this.rows;
        final int column = point.x / cellWidth;
        final int row = point.y / cellHeight;
        final int result = row * this.columns + column;
        return result;
    }

    /**
     * build the array of board cells and the layout manager.
     * @param columns
     * @param rows
     */
    protected void init(final int columns, final int rows) {
        if (SwingUtilities.isEventDispatchThread()) {
            initInternal(columns, rows);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    initInternal(columns, rows);
                }
            });
        }
    }

    private void initInternal(final int columns, final int rows) {
        this.columns = columns;
        this.rows = rows;
        this.cellColors = new int[columns * rows];
        this.cellHighlights = new boolean[this.cellColors.length];
        this.setPreferredSize(new Dimension(columns * DEFAULT_UI_BOARD_CELL_WIDTH, rows * DEFAULT_UI_BOARD_CELL_HEIGHT));
    }

    /**
     * set the colors of all cells.
     * @param cellColors the new colors
     */
    protected void setCellColors(final int[] cellColors) {
        this.cellColors = cellColors;
        this.cellHighlights = new boolean[this.cellColors.length];
        this.repaint();
    }


    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2d = (Graphics2D) g.create();
        final Dimension size = this.getSize();
        final int cellWidth = size.width / this.columns;
        final int cellHeight = size.height / this.rows;
        for (int index = 0, y = 0, row = 0;  row < this.rows;  y += cellHeight, ++row) {
            for (int x = 0, column = 0;  column < this.columns;  x += cellWidth, ++column) {
                final boolean highlight = this.cellHighlights[index];
                final int color = this.cellColors[index++];
                g2d.setColor(COLORS[color]);
                g2d.fillRect(x, y, cellWidth, cellHeight);
                if (highlight) {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(Color.WHITE);
                    g2d.drawOval(x + 2, y + 2, cellWidth - 1 - 2 - 2, cellHeight - 1 - 2 - 2);
                }
            }
        }
    }

    /**
     * set the highlight value of all cells - the ones contained in the specified
     * collection are set to true, all others to false.
     * @param highlightCells
     */
    public void highlightCells(final Collection<Integer> highlightCells) {
        Arrays.fill(this.cellHighlights, false);
        for (final Integer cell : highlightCells) {
            this.cellHighlights[cell.intValue()] = true;
        }
        this.repaint();
    }
}
