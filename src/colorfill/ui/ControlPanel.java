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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import net.java.dev.designgridlayout.DesignGridLayout;

/**
 * this panel contains various controls (buttons etc.)
 */
public class ControlPanel extends JPanel {

    private static final long serialVersionUID = 6465422835992852821L;

    private final ControlController controller;

    private final JButton buttonNew = new JButton();
    private final JLabel labelMove = new JLabel();
    private final JButton buttonUndo = new JButton();
    private final JButton buttonRedo = new JButton();

    /**
     * constructor
     * @param controller
     */
    protected ControlPanel(final ControlController controller) {
        super();
        this.controller = controller;
        final DesignGridLayout layout = new DesignGridLayout(this);
        layout.emptyRow();
        layout.row().grid().add(this.makeButtonNew());
        layout.emptyRow();
        layout.row().grid().add(new JSeparator());
        layout.row().grid().add(this.makeLabelMove());
        layout.row().grid().add(new JSeparator());
        layout.emptyRow();
        layout.row().grid().add(this.makeButtonUndo()).add(this.makeButtonRedo());
        layout.emptyRow();
    }

    private JButton makeButtonNew() {
        this.buttonNew.setText("new"); // TODO L10N
        this.buttonNew.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = -8005560409660217756L;
            @Override
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.controller.userButtonNew();
            }
        });
        return this.buttonNew;
    }

    private JLabel makeLabelMove() {
        this.setLabelMove(0, false);
        return this.labelMove;
    }

    private JButton makeButtonUndo() {
        this.buttonUndo.setText("undo"); // TODO L10N
        this.buttonUndo.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 387954604158984313L;
            @Override
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.controller.userButtonUndo();
            }
        });
        this.buttonUndo.setEnabled(false);
        return this.buttonUndo;
    }

    private JButton makeButtonRedo() {
        this.buttonRedo.setText("redo"); // TODO L10N
        this.buttonRedo.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = -6622821528552016995L;
            @Override
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.controller.userButtonRedo();
            }
        });
        this.buttonRedo.setEnabled(false);
        return this.buttonRedo;
    }

    /**
     * set the text of label "number of moves".
     * @param numSteps
     */
    protected void setLabelMove(final int numSteps, final boolean isFinished) {
        if (SwingUtilities.isEventDispatchThread()) {
            setLabelMoveInternal(numSteps, isFinished);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setLabelMoveInternal(numSteps, isFinished);
                }
            });
        }
    }
    private void setLabelMoveInternal(final int numSteps, final boolean isFinished) {
        this.labelMove.setText("step: " + numSteps + (isFinished ? " - finished!" : "")); // TODO L10N
    }

    /**
     * set the state (enabled) of some buttons.
     * @param canUndoStep
     * @param canRedoStep
     */
    protected void setButtons(final boolean canUndoStep, final boolean canRedoStep) {
        if (SwingUtilities.isEventDispatchThread()) {
            setButtonsInternal(canUndoStep, canRedoStep);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setButtonsInternal(canUndoStep, canRedoStep);
                }
            });
        }
    }
    private void setButtonsInternal(final boolean canUndoStep, final boolean canRedoStep) {
        this.buttonUndo.setEnabled(canUndoStep);
        this.buttonRedo.setEnabled(canRedoStep);
    }
}
