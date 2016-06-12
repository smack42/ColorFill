/*  ColorFill game and solver
    Copyright (C) 2014, 2015, 2016 Michael Henke

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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.Tag;

public class GameIdDialog extends JDialog {

    private static final long serialVersionUID = 5161678512214338603L;

    private static final ResourceBundle L10N = ResourceBundle.getBundle("colorfill-ui");  //L10N = Localization

    private final GameIdController controller;
    private final MainWindow mainWindow;

    private final JTextField textGameId = new JTextField();
    private final JButton buttonOk = new JButton();
    private final JButton buttonCancel = new JButton();
    private final JButton buttonCopy = new JButton();
    private final JButton buttonPaste = new JButton();
    private final JTextField textCheckGameId = new JTextField();

    /**
     * the constructor
     * @param controller
     * @param mainWindow
     */
    protected GameIdDialog(final GameIdController controller, final MainWindow mainWindow) {
        super(mainWindow, true); // modal
        this.controller = controller;
        this.mainWindow = mainWindow;
        this.setTitle(L10N.getString("gameId.Title.txt"));
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        final JPanel panel = new JPanel();
        final DesignGridLayout layout = new DesignGridLayout(panel);
        layout.withoutConsistentWidthAcrossNonGridRows();
        layout.row().grid().add(new JLabel(L10N.getString("gameId.Info1.txt")));
        layout.row().grid().add(new JLabel(L10N.getString("gameId.Info2.txt")));
        layout.emptyRow();
        layout.row().grid().add(this.makeTextGameId());
        layout.row().grid().add(this.makeTextCheckGameId());
        layout.row().left().fill().add(new JSeparator());
        layout.row().bar().add(this.makeButtonCopy(),Tag.HELP)
                            .add(this.makeButtonPaste(),Tag.HELP)
                            .add(this.makeButtonOk(),Tag.OK)
                            .add(this.makeButtonCancel(),Tag.CANCEL);

        this.add(panel);
    }

    private JTextField makeTextGameId() {
        this.textGameId.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                GameIdDialog.this.changedGameId(e.getDocument());
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                GameIdDialog.this.changedGameId(e.getDocument());
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                GameIdDialog.this.changedGameId(e.getDocument());
            }
        });
        this.textGameId.setColumns(1);
        return this.textGameId;
    }

    private JTextField makeTextCheckGameId() {
        this.textCheckGameId.setEditable(false);
        this.textCheckGameId.setEnabled(false);
        this.textCheckGameId.setText("X");
        this.textCheckGameId.setColumns(1);
        return this.textCheckGameId;
    }

    private void changedGameId(final Document doc) {
        try {
            final String gameId = doc.getText(0, doc.getLength());
            final String checkResult = this.controller.getInfoGameId(gameId);
            this.textCheckGameId.setText(checkResult);
        } catch (BadLocationException e) {
            e.printStackTrace(); // unexpected exception
        }
    }

    private JButton makeButtonOk() {
        this.buttonOk.setText(L10N.getString("gameId.btn.OK.txt"));
        this.buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final String result = GameIdDialog.this.controller.applyGameId(GameIdDialog.this.textGameId.getText());
                if (null != result) {
                    // show error message
                    JOptionPane.showMessageDialog(GameIdDialog.this,
                            L10N.getString("gameId.msg.ApplyGameId.Error.txt") + "\n" + result,
                            L10N.getString("gameId.msg.ApplyGameId.Error.title"),
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    // close this dialog
                    GameIdDialog.this.dispose();
                }
            }
        });
        return this.buttonOk;
    }

    private JButton makeButtonCancel() {
        this.buttonCancel.setText(L10N.getString("gameId.btn.Cancel.txt"));
        final ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GameIdDialog.this.dispose();
            }
        };
        this.buttonCancel.addActionListener(actionListener);
        this.getRootPane().registerKeyboardAction(actionListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        return this.buttonCancel;
    }

    private JButton makeButtonCopy() {
        this.buttonCopy.setText(L10N.getString("gameId.btn.Copy.txt"));
        this.buttonCopy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    // copy text to Clipboard
                    final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                    final String data = GameIdDialog.this.textGameId.getText();
                    clip.setContents(new StringSelection(data), null);
                    // just for visual effect
                    GameIdDialog.this.textGameId.selectAll();
                    GameIdDialog.this.textGameId.requestFocusInWindow();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(GameIdDialog.this,
                            L10N.getString("gameId.msg.CopyPaste.Error.txt") + "\n" + ex.toString(),
                            L10N.getString("gameId.msg.CopyPaste.Error.title"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return this.buttonCopy;
    }

    private JButton makeButtonPaste() {
        this.buttonPaste.setText(L10N.getString("gameId.btn.Paste.txt"));
        this.buttonPaste.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    // insert text from Clipboard
                    final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                    final String data = (String) clip.getData(DataFlavor.stringFlavor);
                    GameIdDialog.this.textGameId.setText(data);
                    // just for visual effect
                    GameIdDialog.this.textGameId.requestFocusInWindow();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(GameIdDialog.this,
                            L10N.getString("gameId.msg.CopyPaste.Error.txt") + "\n" + ex.toString(),
                            L10N.getString("gameId.msg.CopyPaste.Error.title"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return this.buttonPaste;
    }

    /**
     * show this modal dialog.
     */
    protected void showDialog() {
        if (SwingUtilities.isEventDispatchThread()) {                        showDialogInternal(); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { showDialogInternal(); } });
    }
    private void showDialogInternal() {
        this.textGameId.setText(this.controller.getCurrentGameId());
        this.textGameId.selectAll();
        this.textGameId.requestFocusInWindow();
        this.getRootPane().setDefaultButton(this.buttonOk);
        this.pack();
        this.setLocationRelativeTo(this.mainWindow);
        this.setVisible(true);
    }

}
