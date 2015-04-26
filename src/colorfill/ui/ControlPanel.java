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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.IRow;

/**
 * this panel contains various controls (buttons etc.)
 */
public class ControlPanel extends JPanel {

    private static final long serialVersionUID = 6465422835992852821L;

    private static final ResourceBundle L10N = ResourceBundle.getBundle("colorfill-ui");  //L10N = Localization

    private final ControlController controller;

    private final JButton buttonNew = new JButton();
    private final JButton buttonPrefs = new JButton();
    private final JRadioButton userRButton = new JRadioButton();
    private final JLabel userMove = new JLabel();
    private final JButton buttonUndo = new JButton();
    private final JButton buttonRedo = new JButton();

    private static final int MAX_NUMBER_COLOR_BUTTONS = 6; // TODO dynamically handle max. number of color buttons
    private final JButton[] buttonColors = new JButton[MAX_NUMBER_COLOR_BUTTONS];
    private int numColors = MAX_NUMBER_COLOR_BUTTONS;

    private static final int MAX_NUMBER_SOLVER_SOLUTIONS = 4; // TODO dynamically handle max. number of solver solutions visible
    private final IRow[]                solverRows1         = new IRow[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final JRadioButton[]        solverRButtons      = new JRadioButton[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final IRow[]                solverRows2         = new IRow[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final JLabel[]              solverMoves         = new JLabel[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final JButton[]             solverPrevButtons   = new JButton[MAX_NUMBER_SOLVER_SOLUTIONS];
    private final JButton[]             solverNextButtons   = new JButton[MAX_NUMBER_SOLVER_SOLUTIONS];
    private int numVisibleSolverSolutions = 0;
    private volatile int selectedSolution = 0; // 0 == user solution, other = solver solutions

    private final Action actionUndoStep = new AbstractAction() {
        private static final long serialVersionUID = -8005560409660217756L;
        public void actionPerformed(ActionEvent e) {
            if (0 == ControlPanel.this.selectedSolution) {
                ControlPanel.this.buttonUndo.requestFocusInWindow();
            } else {
                ControlPanel.this.solverPrevButtons[ControlPanel.this.selectedSolution - 1].requestFocusInWindow();
            }
            ControlPanel.this.controller.userButtonUndo();
        }
    };
    private final Action actionRedoStep = new AbstractAction() {
        private static final long serialVersionUID = 387954604158984313L;
        public void actionPerformed(ActionEvent e) {
            if (0 == ControlPanel.this.selectedSolution) {
                ControlPanel.this.buttonRedo.requestFocusInWindow();
            } else {
                ControlPanel.this.solverNextButtons[ControlPanel.this.selectedSolution - 1].requestFocusInWindow();
            }
            ControlPanel.this.controller.userButtonRedo();
        }
    };

    /**
     * constructor
     * @param controller
     */
    protected ControlPanel(final ControlController controller, final Color[] colors, final int numColors) {
        super();
        this.controller = controller;
        this.numColors = numColors;

        final ButtonGroup bgroup = new ButtonGroup();

        final JPanel panel = new JPanel();
        final DesignGridLayout layout = new DesignGridLayout(panel);
        layout.row().left().add(this.makeButtonNew()).add(this.makeButtonPrefs());
        layout.row().grid().add(new JSeparator());
        layout.row().grid().add(this.makeRButtonUser(bgroup)).add(this.makeLabelMove());
        final IRow rowButtonColors = layout.row().grid();
        for (final JButton button : this.makeButtonColors(colors)) {
            rowButtonColors.add(button);
        }
        layout.row().left().add(this.makeButtonUndo()).add(this.makeButtonRedo());
        layout.row().grid().add(new JSeparator());
        layout.row().grid().add(new JLabel(L10N.getString("ctrl.lbl.SolverResults.txt")));
        this.makeSolverRows(bgroup, layout);
        this.add(panel);
    }

    private JButton makeButtonNew() {
        this.buttonNew.setText(L10N.getString("ctrl.btn.New.txt"));
        this.buttonNew.setMnemonic(KeyEvent.VK_N);
        this.buttonNew.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.controller.userButtonNew();
            }
        });
        return this.buttonNew;
    }

    private JButton makeButtonPrefs() {
        this.buttonPrefs.setText(L10N.getString("ctrl.btn.Preferences.txt"));
        this.buttonPrefs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.controller.userButtonPrefs();
            }
        });
        return this.buttonPrefs;
    }

    private JRadioButton makeRButtonUser(final ButtonGroup bgroup) {
        this.userRButton.setText(L10N.getString("ctrl.btn.YourGame.txt"));
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

    private JButton[] makeButtonColors(final Color[] colors) {
        for (int i = 0;  i < this.buttonColors.length;  ++i) {
            final String i1txt = "" + (i + 1);
            this.buttonColors[i] = new JButton(i1txt);
            //this.buttonColors[i].setContentAreaFilled(false); // L&F specific hack to make background color visible (not required for Nimbus)
            //this.buttonColors[i].setOpaque(true); // L&F specific hack to make background color visible (not required for Nimbus)
            final int color = i;
            final Action action = new AbstractAction() {
                private static final long serialVersionUID = 4116216107977482049L;
                @Override
                public void actionPerformed(ActionEvent e) {
                    ControlPanel.this.buttonColors[color].requestFocusInWindow(); // make key input visible
                    ControlPanel.this.controller.userButtonColor(color);
                }
            };
            this.buttonColors[i].addActionListener(action);
            this.getActionMap().put("ACTION_" + i1txt, action);
            this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(i1txt), "ACTION_" + i1txt);
            this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("NUMPAD" + i1txt), "ACTION_" + i1txt);
        }
        this.setButtonColors(colors, this.numColors);
        return this.buttonColors;
    }

    private JButton makeButtonUndo() {
        this.buttonUndo.setText(L10N.getString("ctrl.btn.UndoStep.txt"));
        this.buttonUndo.addActionListener(this.actionUndoStep);
        this.buttonUndo.setEnabled(false);
        final String actionName = "ACTION_UNDO_STEP";
        this.getActionMap().put(actionName, this.actionUndoStep);
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("MINUS"), actionName);
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SUBTRACT"), actionName);
        return this.buttonUndo;
    }

    private JButton makeButtonRedo() {
        this.buttonRedo.setText(L10N.getString("ctrl.btn.RedoStep.txt"));
        this.buttonRedo.addActionListener(this.actionRedoStep);
        this.buttonRedo.setEnabled(false);
        final String actionName = "ACTION_REDO_STEP";
        this.getActionMap().put(actionName, this.actionRedoStep);
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("PLUS"), actionName);
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ADD"), actionName);
        return this.buttonRedo;
    }

    private void makeSolverRows(final ButtonGroup bgroup, final DesignGridLayout layout) {
        for (int i = 0;  i < MAX_NUMBER_SOLVER_SOLUTIONS;  ++i) {
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
            this.solverMoves[i].setHorizontalAlignment(SwingConstants.CENTER);
            this.solverPrevButtons[i] = new JButton("-");
            this.solverPrevButtons[i].setVisible(false);
            this.solverPrevButtons[i].addActionListener(this.actionUndoStep);
            this.solverNextButtons[i] = new JButton("+");
            this.solverNextButtons[i].setVisible(false);
            this.solverNextButtons[i].addActionListener(this.actionRedoStep);
            this.solverRows1[i] = layout.row().grid();
            this.solverRows1[i].add(this.solverRButtons[i]).hide();
            this.solverRows2[i] = layout.row().grid();
            this.solverRows2[i].add(this.solverPrevButtons[i]).add(this.solverMoves[i]).add(this.solverNextButtons[i]).hide();
        }
    }

    private void setVisibleUserOrSolver() {
        final int sel = this.selectedSolution;
        this.buttonUndo.setVisible(0 == sel);
        this.buttonRedo.setVisible(0 == sel);
        for (int i = 0;  i < this.buttonColors.length;  ++i) {
            this.buttonColors[i].setVisible((0 == sel) && (i < this.numColors));
        }
        for (int i = 1;  i <= this.numVisibleSolverSolutions;  ++i) {
            this.solverMoves[i - 1].setVisible(i == sel);
            if (i == sel) {
                this.solverRows2[i - 1].forceShow();
            } else {
                this.solverRows2[i - 1].hide();
            }
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
        if (SwingUtilities.isEventDispatchThread()) {                        setLabelMoveInternal(numSteps, isFinished); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { setLabelMoveInternal(numSteps, isFinished); } });
    }
    private void setLabelMoveInternal(final int numSteps, final boolean isFinished) {
        final String str = numSteps + (isFinished ? " " + L10N.getString("ctrl.lbl.SolutionFinished.txt") : "");
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
        if (SwingUtilities.isEventDispatchThread()) {                        setButtonsInternal(canUndoStep, canRedoStep); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { setButtonsInternal(canUndoStep, canRedoStep); } });
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
        if (SwingUtilities.isEventDispatchThread()) {                        clearSolverResultsInternal(); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { clearSolverResultsInternal(); } });
    }
    private void clearSolverResultsInternal() {
        for (int i = 0;  i < MAX_NUMBER_SOLVER_SOLUTIONS;  ++i) {
            this.solverPrevButtons[i].setVisible(false);
            this.solverNextButtons[i].setVisible(false);
            this.solverMoves[i].setVisible(false);
            this.solverRows1[i].hide();
            this.solverRows2[i].hide();
        }
        this.numVisibleSolverSolutions = 0;
        this.userRButton.doClick();
    }

    /**
     * add this solver result to control panel.
     * @param str
     */
    protected void addSolverResult(final String str) {
        if (SwingUtilities.isEventDispatchThread()) {                        addSolverResultInternal(str); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { addSolverResultInternal(str); } });
    }
    private void addSolverResultInternal(final String str) {
        if (this.numVisibleSolverSolutions < this.solverRButtons.length) {
            final int i = this.numVisibleSolverSolutions++;
            this.solverRows1[i].forceShow();
            this.solverRows2[i].hide();
            this.solverRButtons[i].setText(str);
            this.solverMoves[i].setText("0");
        }
    }

    /**
     * set the background colors of the color buttons.
     * @param colors
     */
    protected void setButtonColors(final Color[] colors, final int numColors) {
        if (SwingUtilities.isEventDispatchThread()) {                        setButtonColorsInternal(colors, numColors); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { setButtonColorsInternal(colors, numColors); } });
    }
    private void setButtonColorsInternal(final Color[] colors, final int numColors) {
        this.numColors = numColors;
        for (int i = 0;  i < this.buttonColors.length;  ++i) {
            final Color color = (i < colors.length ? colors[i] : Color.WHITE);
            this.buttonColors[i].setBackground(color);
            if (i >= numColors) {
                this.buttonColors[i].setVisible(false);
            }
        }
    }
}
