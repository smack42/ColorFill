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
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.Tag;

public class PreferencesDialog extends JDialog {

    private static final long serialVersionUID = 5636063419915325085L;

    private static final ResourceBundle L10N = ResourceBundle.getBundle("colorfill-ui");  //L10N = Localization

    private final PreferencesController controller;

    private final JSpinner jspinWidth = new JSpinner();
    private final JSpinner jspinHeight = new JSpinner();
    private final JButton buttonOk = new JButton();
    private final JButton buttonCancel = new JButton();

    /**
     * constructor
     * @param controller
     * @param mainWindow
     */
    protected PreferencesDialog(final PreferencesController controller, final MainWindow mainWindow) {
        super(mainWindow, true); // modal
        this.controller = controller;
        this.setTitle(L10N.getString("pref.Title.txt"));

        final JPanel panel = new JPanel();
        final DesignGridLayout layout = new DesignGridLayout(panel);
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.Width.txt"))).add(this.makeJspinWidth());
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.Height.txt"))).add(this.makeJspinHeight());
        layout.emptyRow();
        layout.row().grid().add(new JSeparator());
        layout.emptyRow();
        layout.row().bar().add(this.makeButtonOk(), Tag.OK).add(this.makeButtonCancel(), Tag.CANCEL);

        this.add(panel);
        SwingUtilities.getRootPane(this.buttonOk).setDefaultButton(this.buttonOk);
        this.pack();
        this.setLocationRelativeTo(mainWindow);
        this.setVisible(false);
    }

    private JSpinner makeJspinWidth() {
        this.jspinWidth.setModel(new SpinnerNumberModel(this.controller.getWidth(), 2, 1000, 1)); // TODO preferences min/max "width"
        return this.jspinWidth;
    }

    private JSpinner makeJspinHeight() {
        this.jspinHeight.setModel(new SpinnerNumberModel(this.controller.getHeight(), 2, 1000, 1)); // TODO preferences min/max "height"
        return this.jspinHeight;
    }

    private JButton makeButtonOk() {
        this.buttonOk.setText(L10N.getString("pref.btn.OK.txt"));
        this.buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreferencesDialog.this.controller.setWidth(((Number)PreferencesDialog.this.jspinWidth.getValue()).intValue());
                PreferencesDialog.this.controller.setHeight(((Number)PreferencesDialog.this.jspinHeight.getValue()).intValue());
                PreferencesDialog.this.controller.userPrefsOK();
                PreferencesDialog.this.setVisible(false);
            }
        });
        return this.buttonOk;
    }

    private JButton makeButtonCancel() {
        this.buttonCancel.setText(L10N.getString("pref.btn.Cancel.txt"));
        this.buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreferencesDialog.this.setVisible(false);
            }
        });
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
        this.jspinWidth.setValue(Integer.valueOf(this.controller.getWidth()));
        this.jspinHeight.setValue(Integer.valueOf(this.controller.getHeight()));
        this.setVisible(true);
    }
}
