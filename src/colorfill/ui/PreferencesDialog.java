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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import colorfill.model.StartPositionEnum;

import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.ISpannableGridRow;
import net.java.dev.designgridlayout.Tag;

public class PreferencesDialog extends JDialog {

    private static final long serialVersionUID = 5636063419915325085L;

    private static final ResourceBundle L10N = ResourceBundle.getBundle("colorfill-ui");  //L10N = Localization

    private final PreferencesController controller;
    private final MainWindow mainWindow;

    private final JSpinner jspinWidth = new JSpinner();
    private final JSpinner jspinHeight = new JSpinner();
    private final JComboBox jcomboStartPos = new JComboBox(); // Java 6: rawtype JComboBox
    private final JButton buttonOk = new JButton();
    private final JButton buttonCancel = new JButton();
    private final JRadioButton[] rbuttonsColors;
    private final JCheckBox checkGridLines = new JCheckBox();

    /**
     * constructor
     * @param controller
     * @param mainWindow
     */
    protected PreferencesDialog(final PreferencesController controller, final MainWindow mainWindow) {
        super(mainWindow, true); // modal
        this.controller = controller;
        this.mainWindow = mainWindow;
        this.setTitle(L10N.getString("pref.Title.txt"));

        final JPanel panel = new JPanel();
        final DesignGridLayout layout = new DesignGridLayout(panel);
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.Width.txt"))).addMulti(this.makeJspinWidth());
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.Height.txt"))).addMulti(this.makeJspinHeight());
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.StartPos.txt"))).addMulti(this.makeJcomboStartPos());
        layout.emptyRow();
        layout.row().grid().add(new JSeparator());
        layout.emptyRow();

        layout.row().grid(new JLabel(L10N.getString("pref.lbl.GridLines.txt"))).addMulti(this.makeCheckGridLines());
        layout.emptyRow();
        final Color[][] allUiColors = this.controller.getAllUiColors();
        this.rbuttonsColors = new JRadioButton[allUiColors.length];
        this.makeColorButtons(layout, allUiColors);

        layout.emptyRow();
        layout.row().grid().add(new JSeparator());
        layout.emptyRow();
        layout.row().bar().add(this.makeButtonOk(), Tag.OK).add(this.makeButtonCancel(), Tag.CANCEL);

        this.add(panel);
    }

    private JSpinner makeJspinWidth() {
        this.jspinWidth.setModel(new SpinnerNumberModel(this.controller.getWidth(), 2, 1000, 1)); // TODO preferences min/max "width"
        return this.jspinWidth;
    }

    private JSpinner makeJspinHeight() {
        this.jspinHeight.setModel(new SpinnerNumberModel(this.controller.getHeight(), 2, 1000, 1)); // TODO preferences min/max "height"
        return this.jspinHeight;
    }

    private JComboBox makeJcomboStartPos() { // Java 6: rawtype JComboBox
        for (final StartPositionEnum spe : StartPositionEnum.values()) {
            this.jcomboStartPos.addItem(new StartPosItem(spe));
        }
        return this.jcomboStartPos;
    }

    private JCheckBox makeCheckGridLines() {
        return this.checkGridLines;
    }

    private void makeColorButtons(final DesignGridLayout layout, final Color[][] allUiColors) {
        final ButtonGroup bgroup = new ButtonGroup();
        int i = 0;
        for (final Color[] colorScheme : allUiColors) {
            final int colorSchemeNumber = i++;
            final ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PreferencesDialog.this.rbuttonsColors[colorSchemeNumber].setSelected(true);
                }
            };
            final JLabel label = new JLabel(L10N.getString("pref.lbl.ColorScheme.txt"));
            this.rbuttonsColors[colorSchemeNumber] = new JRadioButton("" + (colorSchemeNumber + 1));
            bgroup.add(this.rbuttonsColors[colorSchemeNumber]);
            this.rbuttonsColors[colorSchemeNumber].addActionListener(actionListener);
            final ISpannableGridRow row = layout.row().grid(label).add(this.rbuttonsColors[colorSchemeNumber]);
            for (int color = 0;  color < colorScheme.length;  ++color) {
                final JButton button = new JButton("" + (color + 1));
                button.addActionListener(actionListener);
                button.setBackground(colorScheme[color]);
                row.add(button);
            }
        }
    }

    private JButton makeButtonOk() {
        this.buttonOk.setText(L10N.getString("pref.btn.OK.txt"));
        this.buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int colorSchemeNumber = 0;
                for (int i = 0;  i < PreferencesDialog.this.rbuttonsColors.length;  ++i) {
                    if (PreferencesDialog.this.rbuttonsColors[i].isSelected()) {
                        colorSchemeNumber = i;
                    }
                }
                PreferencesDialog.this.controller.userPrefsOK(
                        ((Number)PreferencesDialog.this.jspinWidth.getValue()).intValue(),
                        ((Number)PreferencesDialog.this.jspinHeight.getValue()).intValue(),
                        ((StartPosItem)PreferencesDialog.this.jcomboStartPos.getSelectedItem()).spe,
                        PreferencesDialog.this.checkGridLines.isSelected(),
                        colorSchemeNumber);
                PreferencesDialog.this.setVisible(false);
            }
        });
        this.getRootPane().setDefaultButton(this.buttonOk);
        return this.buttonOk;
    }

    private JButton makeButtonCancel() {
        this.buttonCancel.setText(L10N.getString("pref.btn.Cancel.txt"));
        final ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreferencesDialog.this.setVisible(false);
            }
        };
        this.buttonCancel.addActionListener(actionListener);
        this.getRootPane().registerKeyboardAction(actionListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        return this.buttonCancel;
    }

    /**
     * show this modal dialog.
     */
    protected void showDialog() {
        if (SwingUtilities.isEventDispatchThread()) {                        showDialogInternal(); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { showDialogInternal(); } });
    }
    private void showDialogInternal() {
        this.rbuttonsColors[this.controller.getUiColorsNumber()].setSelected(true);
        this.jspinWidth.setValue(Integer.valueOf(this.controller.getWidth()));
        this.jspinHeight.setValue(Integer.valueOf(this.controller.getHeight()));
        this.jcomboStartPos.setSelectedIndex(this.controller.getStartPos().ordinal());
        this.checkGridLines.setSelected(this.controller.isShowGridLines());
        this.pack();
        this.setLocationRelativeTo(this.mainWindow);
        this.setVisible(true);
    }



    private static class StartPosItem {
        private final StartPositionEnum spe;
        private final String l10nString;

        private StartPosItem(StartPositionEnum spe) {
            this.spe = spe;
            this.l10nString = PreferencesDialog.L10N.getString(spe.getL10nKey());
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return this.l10nString;
        }
    }
}
