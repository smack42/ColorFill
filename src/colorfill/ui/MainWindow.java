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

package colorfill.ui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * the main window of the program.
 */
public class MainWindow extends JFrame {

    private static final long serialVersionUID = -708157957994420526L;

    /**
     * constructor
     * @param windowTitle
     * @param boardPanel
     * @param controlPanel
     */
    protected MainWindow(final String windowTitle, final JPanel boardPanel, final JPanel controlPanel) {
        super(windowTitle);
        this.getContentPane().add(boardPanel, BorderLayout.CENTER);
        this.getContentPane().add(controlPanel, BorderLayout.EAST);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * resize (pack) the main window
     */
    protected void update() {
        if (SwingUtilities.isEventDispatchThread()) {                          updateInternal(); }
        else { SwingUtilities.invokeLater(new Runnable() { public void run() { updateInternal(); } }); }
    }
    private void updateInternal() {
        this.pack();
        this.setVisible(true);
    }
}
