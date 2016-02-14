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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    private final JButton buttonHint = new JButton();
    private final JButton buttonHintColor = new JButton();
    private final JLabel  hintEstimatedSteps = new JLabel();
    private boolean showHint = false;

    private final String[]          solverNames;
    private final IRow[]            solverRows1;
    private final JRadioButton[]    solverRButtons;
    private final IRow[]            solverRows2;
    private final JLabel[]          solverMoves;
    private final JButton[]         solverPrevButtons;
    private final JButton[]         solverNextButtons;
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
    private final Action actionHint = new AbstractAction() {
        private static final long serialVersionUID = -6622821528552016995L;
        public void actionPerformed(ActionEvent e) {
            if (0 == ControlPanel.this.selectedSolution) {
                ControlPanel.this.buttonHint.requestFocusInWindow();
                ControlPanel.this.controller.userButtonHint();
            }
        }
    };

    /**
     * constructor
     * @param controller
     */
    protected ControlPanel(final ControlController controller, final Color[] colors, final int numColors, final String[] solverNames) {
        super();
        this.controller = controller;
        this.numColors = numColors;

        this.solverNames        = solverNames;
        this.solverRows1        = new IRow[solverNames.length];
        this.solverRButtons     = new JRadioButton[solverNames.length];
        this.solverRows2        = new IRow[solverNames.length];
        this.solverMoves        = new JLabel[solverNames.length];
        this.solverPrevButtons  = new JButton[solverNames.length];
        this.solverNextButtons  = new JButton[solverNames.length];

        final ButtonGroup bgroup = new ButtonGroup();

        final JPanel panel = new JPanel();
        final DesignGridLayout layout = new DesignGridLayout(panel);
        layout.row().grid().add(this.makeButtonNew(), 3).add(this.makeButtonPrefs(), 3);
        layout.row().grid().add(new JSeparator());
        layout.row().grid().add(this.makeRButtonUser(bgroup), 4).add(this.makeLabelMove()).empty();
        final IRow rowButtonColors = layout.row().grid();
        for (final JButton button : this.makeButtonColors(colors)) {
            rowButtonColors.add(button);
        }
        layout.row().grid().add(this.makeButtonUndo(), 3).add(this.makeButtonRedo(), 3);
        layout.row().grid().add(this.makeButtonHint(), 3).empty().add(this.makeHintEstimatedSteps()).add(this.makeButtonHintColor());
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

    private JButton makeButtonHint() {
        this.buttonHint.setText(L10N.getString("ctrl.btn.Hint.txt"));
        this.buttonHint.addActionListener(this.actionHint);
        final String actionName = "ACTION_HINT";
        this.getActionMap().put(actionName, this.actionHint);
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("H"), actionName);
        return this.buttonHint;
    }

    private JButton makeButtonHintColor() {
        this.buttonHintColor.setText("1");
        this.buttonHintColor.setVisible(this.showHint);
        this.buttonHintColor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    final int hintColor = Integer.parseInt(ControlPanel.this.buttonHintColor.getText()) - 1;
                    ControlPanel.this.buttonColors[hintColor].doClick();
                } catch (Exception ignored) {}
            }
        });
        this.buttonHintColor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                try {
                    final int hintColor = Integer.parseInt(ControlPanel.this.buttonHintColor.getText()) - 1;
                    ControlPanel.this.controller.userHintColor(hintColor);
                } catch (Exception ignored) {}
            }
            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                ControlPanel.this.controller.userHintColor(-1);
            }
        });
        return this.buttonHintColor;
    }

    private JLabel makeHintEstimatedSteps() {
        this.hintEstimatedSteps.setText("99");
        this.hintEstimatedSteps.setVisible(this.showHint);
        return this.hintEstimatedSteps;
    }

    private void makeSolverRows(final ButtonGroup bgroup, final DesignGridLayout layout) {
        for (int i = 0;  i < this.solverNames.length;  ++i) {
            this.solverRButtons[i] = new JRadioButton("?? " + this.solverNames[i]);
            this.solverRButtons[i].setEnabled(false);
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
            this.solverRows1[i].add(this.solverRButtons[i]);
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
        this.buttonHint.setVisible(0 == sel);
        this.buttonHintColor.setVisible((0 == sel) && this.showHint);
        this.hintEstimatedSteps.setVisible((0 == sel) && this.showHint);
        if (this.solverMoves[0] != null) { // don't do this during constructor
            for (int i = 1;  i <= this.solverNames.length;  ++i) {
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
        for (int i = 0;  i < this.solverNames.length;  ++i) {
            this.solverPrevButtons[i].setVisible(false);
            this.solverNextButtons[i].setVisible(false);
            this.solverMoves[i].setVisible(false);
            this.solverRButtons[i].setEnabled(false);
            this.solverRButtons[i].setText("?? " + this.solverNames[i]);
            this.solverRows2[i].hide();
        }
        this.userRButton.doClick();
    }

    /**
     * add this solver result to control panel.
     * @param str
     */
    protected void addSolverResult(final int num, final String str) {
        if (SwingUtilities.isEventDispatchThread()) {                        addSolverResultInternal(num, str); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { addSolverResultInternal(num, str); } });
    }
    private void addSolverResultInternal(final int numMoves, final String solverName) {
        int i;
        for (i = 0;  i < solverNames.length;  ++i) {
            if (solverNames[i].equals(solverName)) {
                break;
            }
        }
        if (i < solverNames.length) { // solverName found
            this.solverRows2[i].hide();
            if (numMoves > 0) {
                this.solverRButtons[i].setText(numMoves + " " + solverName);
                this.solverRButtons[i].setEnabled(true);
            } else { // no solution
                this.solverRButtons[i].setText("--- " + solverName);
                this.solverRButtons[i].setEnabled(false);
            }
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
        }
        this.setVisibleUserOrSolver();
        try {
            final int hintColor = Integer.parseInt(this.buttonHintColor.getText()) - 1;
            this.buttonHintColor.setBackground(this.buttonColors[hintColor].getBackground());
        } catch (Exception ignored) {}
    }

    /**
     * show the hint.
     * @param color
     * @param stepsToDo
     */
    protected void showHint(final Integer color, final Integer estimatedSteps) {
        if (SwingUtilities.isEventDispatchThread()) {                        showHintInternal(color, estimatedSteps); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { showHintInternal(color, estimatedSteps); } });
    }
    private void showHintInternal(final Integer color, final Integer estimatedSteps) {
        if ((null != color) && (null != estimatedSteps)) {
            this.buttonHintColor.setText(String.valueOf(color.intValue() + 1));
            this.buttonHintColor.setBackground(this.buttonColors[color.intValue()].getBackground());
            this.hintEstimatedSteps.setText(estimatedSteps.toString());
            this.showHint = true;
            this.setVisibleUserOrSolver();
        }
    }

    /**
     * hide the hint.
     */
    protected void hideHint() {
        if (SwingUtilities.isEventDispatchThread()) {                        hideHintInternal(); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { hideHintInternal(); } });
    }
    private void hideHintInternal() {
        this.showHint = false;
        this.setVisibleUserOrSolver();
    }
}
