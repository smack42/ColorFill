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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * representation of the board with its colored cells.
 */
public class BoardPanel extends JPanel {

    private static final long serialVersionUID = 8760536779314645208L;

    public static final int DEFAULT_UI_BOARD_CELL_WIDTH  = 32;
    public static final int DEFAULT_UI_BOARD_CELL_HEIGHT = 32;

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
    private BoardCell[] boardCells;

    /**
     * constructor
     * @param controller
     */
    protected BoardPanel(final BoardController controller) {
        super();
        this.controller = controller;
    }

    /**
     * build the array of board cells and the layout manager.
     * @param width number of columns on the board
     * @param height number of rows on the board
     */
    protected void init(final int width, final int height) {
        if (SwingUtilities.isEventDispatchThread()) {
            initInternal(width, height);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    initInternal(width, height);
                }
            });
        }
    }

    private void initInternal(final int width, final int height) {
        this.removeAll();
        this.setLayout(new GridLayoutSquare(height, width));
        this.boardCells = new BoardCell[width * height];
        for (int i = 0;  i < this.boardCells.length;  ++i) {
            this.boardCells[i] = new BoardCell(i);
            this.add(this.boardCells[i]);
        }
    }

    /**
     * change the color of the specified cell (at index).
     * causes a repaint() of the board cell if the color was
     * changed to a different value than before.
     * @param index the cell
     * @param color the new color value
     */
    protected void setCellColor(final int index, final int color) {
        this.boardCells[index].setColor(color);
    }



    private class BoardCell extends JPanel implements MouseListener {

        private static final long serialVersionUID = -345620131879646633L;

        private final int index;
        private int color = 0;

        private BoardCell(final int index) {
            super(false);
            this.index = index;
            this.setPreferredSize(new Dimension(DEFAULT_UI_BOARD_CELL_WIDTH, DEFAULT_UI_BOARD_CELL_HEIGHT));
            this.setOpaque(true);
            this.addMouseListener(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            final Graphics2D g2d = (Graphics2D) g.create();
            final Dimension size = this.getSize();
            g2d.setColor(COLORS[this.color]);
            g2d.fillRect(0, 0, size.width, size.height);
        }

        private void setColor(final int color) {
            if (this.color != color) {
                this.color = color;
                this.repaint();
            }
        }

        // implements MouseListener
        @Override
        public void mouseClicked(MouseEvent e) {
            BoardPanel.this.controller.userClickedOnCell(e, this.index, this.color);
        }
        @Override
        public void mousePressed(MouseEvent e) {
            // no-op
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            // no-op
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            // no-op
        }
        @Override
        public void mouseExited(MouseEvent e) {
            // no-op
        }
    }
}
