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
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
    private final JRadioButton userRButton = new JRadioButton();
    private final JLabel userMove = new JLabel();
    private final JButton buttonUndo = new JButton();
    private final JButton buttonRedo = new JButton();

    private static final int MAX_NUMBER_SOLVER_SOLUTIONS = 3; // TODO dynamically handle max. number of solver solutions visible
    private final JPanel[]              solverPanels        = new JPanel[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final DesignGridLayout[]    solverLayouts       = new DesignGridLayout[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final JRadioButton[]        solverRButtons      = new JRadioButton[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final JLabel[]              solverMoves         = new JLabel[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final JButton[]             solverPrevButtons   = new JButton[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final JButton[]             solverNextButtons   = new JButton[MAX_NUMBER_SOLVER_SOLUTIONS];
    private int numVisibleSolverSolutions = 0;
    private volatile int selectedSolution = 0; // 0 == user solution, other = solver solutions

    /**
     * constructor
     * @param controller
     */
    protected ControlPanel(final ControlController controller) {
        super();
        this.controller = controller;

        final ButtonGroup bgroup = new ButtonGroup();

        final JPanel userPanel = new JPanel();
        final DesignGridLayout userLayout = new DesignGridLayout(userPanel);
        userLayout.row().grid().add(this.makeButtonNew());
        userLayout.emptyRow();
        userLayout.row().grid().add(this.makeRButtonUser(bgroup));
        userLayout.row().grid().add(this.makeButtonUndo()).add(this.makeLabelMove()).add(this.makeButtonRedo());
        userLayout.emptyRow();
        userLayout.row().grid().add(new JSeparator());
        userLayout.row().grid().add(new JLabel("solver results")).add(new JLabel("-------------------------")); // TODO L10N

        for (int i = 0;  i < MAX_NUMBER_SOLVER_SOLUTIONS;  ++i) {
            this.solverPanels[i] = new JPanel();
            this.solverPanels[i].setVisible(false);
            this.solverLayouts[i] = new DesignGridLayout(this.solverPanels[i]);
            this.solverRButtons[i] = new JRadioButton();
            bgroup.add(this.solverRButtons[i]);
            final int numProgress = i + 1;
            this.solverRButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ControlPanel.this.selectedSolution = numProgress;
                    ControlPanel.this.setVisibleUserOrSolver();
                    ControlPanel.this.controller.userButtonSolution(numProgress);
                }
            });
            this.solverMoves[i] = new JLabel();
            this.solverMoves[i].setVisible(false);
            this.solverPrevButtons[i] = new JButton("\u2190"); // \u2190 == leftwardsArrow "<"
            this.solverPrevButtons[i].setVisible(false);
            this.solverPrevButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ControlPanel.this.controller.userButtonUndo();
                }
            });
            this.solverNextButtons[i] = new JButton("\u2192"); // \u2192 == rightwardsArrow ">"
            this.solverNextButtons[i].setVisible(false);
            this.solverNextButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ControlPanel.this.controller.userButtonRedo();
                }
            });
            this.solverLayouts[i].row().grid().add(this.solverRButtons[i]);
            this.solverLayouts[i].row().grid().add(this.solverPrevButtons[i]).add(this.solverMoves[i]).add(this.solverNextButtons[i]);
        }

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(userPanel);
        for (final JPanel solverPanel : this.solverPanels) {
            this.add(solverPanel);
        }
    }

    private JButton makeButtonNew() {
        this.buttonNew.setText("new"); // TODO L10N
        this.buttonNew.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.controller.userButtonNew();
            }
        });
        return this.buttonNew;
    }

    private JRadioButton makeRButtonUser(final ButtonGroup bgroup) {
        this.userRButton.setText("your moves"); // TODO L10N
        this.userRButton.setSelected(true);
        bgroup.add(this.userRButton);
        this.userRButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.selectedSolution = 0;
                ControlPanel.this.setVisibleUserOrSolver();
                ControlPanel.this.controller.userButtonSolution(0);
            }
        });
        return this.userRButton;
    }

    private JLabel makeLabelMove() {
        this.setLabelMove(0, false);
        return this.userMove;
    }

    private JButton makeButtonUndo() {
        this.buttonUndo.setText("undo"); // TODO L10N
        this.buttonUndo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.controller.userButtonUndo();
            }
        });
        this.buttonUndo.setEnabled(false);
        return this.buttonUndo;
    }

    private JButton makeButtonRedo() {
        this.buttonRedo.setText("redo"); // TODO L10N
        this.buttonRedo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.controller.userButtonRedo();
            }
        });
        this.buttonRedo.setEnabled(false);
        return this.buttonRedo;
    }

    private void setVisibleUserOrSolver() {
        final int sel = this.selectedSolution;
        this.buttonUndo.setVisible(0 == sel);
        this.buttonRedo.setVisible(0 == sel);
        for (int i = 1;  i <= this.numVisibleSolverSolutions;  ++i) {
            this.solverPrevButtons[i - 1].setVisible(i == sel);
            this.solverNextButtons[i - 1].setVisible(i == sel);
            this.solverMoves[i - 1].setVisible(i == sel);
        }
    }

    /**
     * set the text of label "number of moves".
     * @param numSteps
     */
    protected void setLabelMove(final int numSteps, final boolean isFinished) {
        if (SwingUtilities.isEventDispatchThread()) {
            setLabelMoveInternal(numSteps, isFinished);
        } else SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setLabelMoveInternal(numSteps, isFinished);
            }
        });
    }
    private void setLabelMoveInternal(final int numSteps, final boolean isFinished) {
        final String str = numSteps + (isFinished ? " !!!" : ""); // TODO L10N
        if (0 == this.selectedSolution) {
            this.userMove.setText(str);
        } else {
            this.solverMoves[this.selectedSolution - 1].setText(str);
        }
    }

    /**
     * set the state (enabled) of some buttons.
     * @param canUndoStep
     * @param canRedoStep
     */
    protected void setButtons(final boolean canUndoStep, final boolean canRedoStep) {
        if (SwingUtilities.isEventDispatchThread()) {
            setButtonsInternal(canUndoStep, canRedoStep);
        } else SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setButtonsInternal(canUndoStep, canRedoStep);
            }
        });
    }
    private void setButtonsInternal(final boolean canUndoStep, final boolean canRedoStep) {
        if (0 == this.selectedSolution) {
            this.buttonUndo.setEnabled(canUndoStep);
            this.buttonRedo.setEnabled(canRedoStep);
        } else {
            this.solverPrevButtons[this.selectedSolution - 1].setEnabled(canUndoStep);
            this.solverNextButtons[this.selectedSolution - 1].setEnabled(canRedoStep);
        }
    }

    /**
     * remove all solver results from control panel.
     */
    protected void clearSolverResults() {
        if (SwingUtilities.isEventDispatchThread()) {
            clearSolverResultsInternal();
        } else SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                clearSolverResultsInternal();
            }
        });
    }
    private void clearSolverResultsInternal() {
        for (int i = 0;  i < MAX_NUMBER_SOLVER_SOLUTIONS;  ++i) {
            this.solverPrevButtons[i].setVisible(false);
            this.solverNextButtons[i].setVisible(false);
            this.solverMoves[i].setVisible(false);
            this.solverPanels[i].setVisible(false);
        }
        this.numVisibleSolverSolutions = 0;
        this.userRButton.doClick();
    }

    /**
     * add this solver result to control panel.
     * @param str
     */
    protected void addSolverResult(final String str) {
        if (SwingUtilities.isEventDispatchThread()) {
            addSolverResultInternal(str);
        } else SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                addSolverResultInternal(str);
            }
        });
    }
    private void addSolverResultInternal(final String str) {
        if (this.numVisibleSolverSolutions < this.solverRButtons.length) {
            final int i = this.numVisibleSolverSolutions++;
            this.solverRButtons[i].setText(str);
            this.solverPanels[i].setVisible(true);
            this.solverMoves[i].setText("0");
        }
    }
}
