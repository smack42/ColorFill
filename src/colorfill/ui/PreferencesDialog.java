/*  ColorFill game and solver
    Copyright (C) 2014 - 2025 Michael Henke

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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import colorfill.model.BoardColorNumbersEnum;
import colorfill.model.GridLinesEnum;
import colorfill.model.HighlightColorEnum;
import colorfill.model.StartPositionEnum;

import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.Tag;

public class PreferencesDialog extends JDialog {

    private static final long serialVersionUID = 5636063419915325085L;

    private static final ResourceBundle L10N = ResourceBundle.getBundle("colorfill-ui");  //L10N = Localization

    private final PreferencesController controller;
    private final MainWindow mainWindow;
    private final Color[][] allUiColors;

    private final JFormattedTextField jftextWidth = new JFormattedTextField();
    private final JFormattedTextField jftextHeight = new JFormattedTextField();
    private final JFormattedTextField jftextNumColors = new JFormattedTextField();
    private final JFormattedTextField jftextCellSize = new JFormattedTextField();
    private final JComboBox<String> jcomboStartPos = new JComboBox<>();
    private final JButton buttonOk = new JButton();
    private final JButton buttonCancel = new JButton();
    private final JButton buttonDefaults = new JButton();
    private final JComboBox<Integer> jcomboColorSchemes = new JComboBox<>();
    private final JRadioButton[] jrbuttonGridLines = new JRadioButton[GridLinesEnum.values().length];
    private final JRadioButton[] jrbuttonBoardColorNumbers = new JRadioButton[BoardColorNumbersEnum.values().length];
    private final JRadioButton[] jrbuttonHighlightColor = new JRadioButton[HighlightColorEnum.values().length];
    private final JFormattedTextField jftextHighlightTransparency = new JFormattedTextField();
    private final JComboBox<String> jcomboLookAndFeel = new JComboBox<>();
    private final JRadioButton[] jrbuttonControlPanelEast = new JRadioButton[2];

    private boolean closedByOkButton = false;
    private boolean doUiPreview = false;


    /**
     * constructor
     * @param controller
     * @param mainWindow
     */
    protected PreferencesDialog(final PreferencesController controller, final MainWindow mainWindow,
            final String progname, final String version, final String author, final Color[][] allUiColors) {
        super(mainWindow, true); // modal
        this.controller = controller;
        this.mainWindow = mainWindow;
        this.allUiColors = allUiColors;
        this.setTitle(L10N.getString("pref.Title.txt"));
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        final JPanel panel = new JPanel();
        final DesignGridLayout layout = new DesignGridLayout(panel);
        layout.withoutConsistentWidthAcrossNonGridRows();
        layout.row().left().addMulti(new JLabel(progname + " " + version));
        layout.row().left().addMulti(new JLabel(author));
        layout.row().left().fill().add(new JSeparator());
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.Width.txt"))).addMulti(this.makeTextfieldSlider(this.jftextWidth, 2, 100)); // TODO preferences min/max "width"
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.Height.txt"))).addMulti(this.makeTextfieldSlider(this.jftextHeight, 2, 100)); // TODO preferences min/max "height"
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.NumColors.txt"))).addMulti(this.makeTextfieldSlider(this.jftextNumColors, 2, 6)); // TODO preferences min/max "numColors"
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.StartPos.txt"))).addMulti(this.makeJcomboStartPos());
        layout.row().left().fill().add(new JSeparator());
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.LookAndFeel.txt"))).addMulti(this.makeJcomboLookAndFeel());
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.ControlPanelEast.txt"))).add(this.makeRadiobuttonsControlPanelEast()).empty();
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.CellSize.txt"))).addMulti(this.makeTextfieldSlider(this.jftextCellSize, 8, 150)); // TODO preferences min/max "cellSize"
        layout.row().left().fill().add(new JSeparator());
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.ColorScheme.txt"))).addMulti(this.makeJcomboColorSchemes());
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.GridLines.txt"))).add(this.makeRadiobuttonsEnum(this.jrbuttonGridLines, GridLinesEnum.class));
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.BoardColorNumbers.txt"))).add(makeRadiobuttonsEnum(this.jrbuttonBoardColorNumbers, BoardColorNumbersEnum.class));
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.HighlightColor.txt"))).add(makeRadiobuttonsEnum(this.jrbuttonHighlightColor, HighlightColorEnum.class));
        layout.row().grid(new JLabel(L10N.getString("pref.lbl.HighlightTransparency.txt"))).addMulti(this.makeTextfieldSlider(this.jftextHighlightTransparency, 0, 100));
        layout.row().left().fill().add(new JSeparator());
        layout.row().left().addMulti(this.makeButtonDefaults());
        layout.row().bar().add(this.makeButtonOk(), Tag.OK).add(this.makeButtonCancel(), Tag.CANCEL);

        this.add(panel);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // Cancel or Close button: undo preview of color scheme
                if (false == PreferencesDialog.this.closedByOkButton) {
                    PreferencesDialog.this.controller.userPreviewUiColors(
                            PreferencesDialog.this.controller.getUiColorsNumber(),
                            PreferencesDialog.this.controller.getGridLines(),
                            PreferencesDialog.this.controller.getBoardColorNumbers(),
                            PreferencesDialog.this.controller.getNumColors(),
                            PreferencesDialog.this.controller.getHighlightColor(),
                            PreferencesDialog.this.controller.getHighlightTransparency());
                }
            }
        });
    }

    private JComboBox<String> makeJcomboStartPos() {
        for (final StartPositionEnum spe : StartPositionEnum.values()) {
            this.jcomboStartPos.addItem(L10N.getString(spe.getL10nKey()));
        }
        return this.jcomboStartPos;
    }

    private JComboBox<String> makeJcomboLookAndFeel() {
        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            this.jcomboLookAndFeel.addItem(info.getName());
        }
        return this.jcomboLookAndFeel;
    }

    private JRadioButton[] makeRadiobuttonsControlPanelEast() {
        final ButtonGroup bgroup = new ButtonGroup();
        JRadioButton jrb = new JRadioButton(L10N.getString("pref.ControlPanelEast.left.txt"));
        this.jrbuttonControlPanelEast[0] = jrb;
        bgroup.add(jrb);
        jrb = new JRadioButton(L10N.getString("pref.ControlPanelEast.right.txt"));
        this.jrbuttonControlPanelEast[1] = jrb;
        bgroup.add(jrb);
        return this.jrbuttonControlPanelEast;
    }

    private <E extends Enum<E>> JRadioButton[] makeRadiobuttonsEnum(JRadioButton[] jrButtons, Class<E> enumClass) {
        final ButtonGroup bgroup = new ButtonGroup();
        for (Enum<E> e : enumClass.getEnumConstants()) {
            final JRadioButton jrb = new JRadioButton(L10N.getString("pref." + enumClass.getSimpleName() + "." + e.name() + ".txt"));
            bgroup.add(jrb);
            jrButtons[e.ordinal()] = jrb;
            jrb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    PreferencesDialog.this.userPreviewUiColors();
                }
            });
        }
        return jrButtons;
    }

    private JComponent[] makeTextfieldSlider(final JFormattedTextField jft, final int min, final int max) {
        final JSlider slider = new JSlider();
        NumberFormatter nf = new NumberFormatter(NumberFormat.getIntegerInstance());
        nf.setMinimum(Integer.valueOf(min));
        nf.setMaximum(Integer.valueOf(max));
        jft.setFormatterFactory(new DefaultFormatterFactory(nf));
        jft.setColumns(3);
        jft.setHorizontalAlignment(JFormattedTextField.TRAILING);
        jft.setValue(Integer.valueOf(min));
        jft.addPropertyChangeListener("value", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                int value = ((Number)jft.getValue()).intValue();
                if (value != slider.getValue()) {
                    slider.setValue(value);
                }
                PreferencesDialog.this.userPreviewUiColors();
            }
        });
        slider.setMinimum(min);
        slider.setMaximum(max);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                jft.setValue(Integer.valueOf(slider.getValue()));
            }
        });
        return new JComponent[] {jft, slider};
    }

    private JComboBox<Integer> makeJcomboColorSchemes() {
        for (int i = 1;  i <= this.allUiColors.length;  ++i) {
            this.jcomboColorSchemes.addItem(Integer.valueOf(i));
        }
        this.jcomboColorSchemes.setRenderer(new ColorSchemeComboBoxRenderer());
        this.jcomboColorSchemes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PreferencesDialog.this.userPreviewUiColors();
            }
        });
        return this.jcomboColorSchemes;
    }


    private class ColorSchemeComboBoxRenderer extends JLabel implements ListCellRenderer<Object> {
        private static final long serialVersionUID = -5760355417428060551L;
        private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(final JList<?> list, final Object value,
                final int index, final boolean isSelected, final boolean cellHasFocus) {
            final JLabel renderer = (JLabel)this.defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final int uiColorsIndex = ((Integer)value).intValue() - 1;
            renderer.setIcon(new ColorSchemeIcon(uiColorsIndex));
            renderer.setHorizontalTextPosition(JLabel.LEADING);
            return renderer;
        }

        private class ColorSchemeIcon implements Icon {
            private final int uiColorsIndex;
            private static final int SIZE = 24; // TODO icon size adapting to JLabel text height
            public ColorSchemeIcon(final int uiColorsIndex) {
                this.uiColorsIndex = uiColorsIndex;
            }
            @Override
            public int getIconWidth() {
                return SIZE * PreferencesDialog.this.allUiColors[this.uiColorsIndex].length;
            }
            @Override
            public int getIconHeight() {
                return SIZE;
            }
            @Override
            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
                int i = 0;
                for (final Color color : PreferencesDialog.this.allUiColors[this.uiColorsIndex]) {
                    g.setColor(color);
                    g.fillRect(x + i*SIZE, y, SIZE, SIZE);
                    ++i;
                }
            }
        }
    }


    private JButton makeButtonOk() {
        this.buttonOk.setText(L10N.getString("pref.btn.OK.txt"));
        this.buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreferencesDialog.this.controller.userPrefsOK(
                        ((Number)PreferencesDialog.this.jftextWidth.getValue()).intValue(),
                        ((Number)PreferencesDialog.this.jftextHeight.getValue()).intValue(),
                        PreferencesDialog.this.getNumColors(),
                        StartPositionEnum.values()[PreferencesDialog.this.jcomboStartPos.getSelectedIndex()],
                        PreferencesDialog.this.getSelectedGridLinesEnum(),
                        PreferencesDialog.this.getSelectedBoardColorNumbersEnum(),
                        PreferencesDialog.this.getSelectedColorSchemeNumber(),
                        ((Number)PreferencesDialog.this.jftextCellSize.getValue()).intValue(),
                        PreferencesDialog.this.getHighlightColorEnum(),
                        PreferencesDialog.this.jcomboLookAndFeel.getItemAt(PreferencesDialog.this.jcomboLookAndFeel.getSelectedIndex()),
                        ((Number)PreferencesDialog.this.jftextHighlightTransparency.getValue()).intValue(),
                        PreferencesDialog.this.jrbuttonControlPanelEast[1].isSelected() );
                PreferencesDialog.this.closedByOkButton = true;
                PreferencesDialog.this.dispose();
            }
        });
        return this.buttonOk;
    }

    private JButton makeButtonCancel() {
        this.buttonCancel.setText(L10N.getString("pref.btn.Cancel.txt"));
        final ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreferencesDialog.this.dispose();
            }
        };
        this.buttonCancel.addActionListener(actionListener);
        this.getRootPane().registerKeyboardAction(actionListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        return this.buttonCancel;
    }

    private JButton makeButtonDefaults() {
        this.buttonDefaults.setText(L10N.getString("pref.btn.Defaults.txt"));
        final ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreferencesDialog.this.controller.userDefaults();
            }
        };
        this.buttonDefaults.addActionListener(actionListener);
        return this.buttonDefaults;
    }

    private void userPreviewUiColors() {
        if (this.doUiPreview) this.controller.userPreviewUiColors(
                this.getSelectedColorSchemeNumber(),
                this.getSelectedGridLinesEnum(),
                this.getSelectedBoardColorNumbersEnum(),
                this.getNumColors(),
                this.getHighlightColorEnum(),
                this.getHighlightTransparency() );
    }

    private int getSelectedColorSchemeNumber() {
        return this.jcomboColorSchemes.getSelectedIndex();
    }

    private GridLinesEnum getSelectedGridLinesEnum() {
        for (GridLinesEnum e : GridLinesEnum.values()) {
            if (this.jrbuttonGridLines[e.ordinal()].isSelected()) return e;
        }
        return null; // shouldn't happen
    }

    private BoardColorNumbersEnum getSelectedBoardColorNumbersEnum() {
        for (BoardColorNumbersEnum e : BoardColorNumbersEnum.values()) {
            if (this.jrbuttonBoardColorNumbers[e.ordinal()].isSelected()) return e;
        }
        return null; // shouldn't happen
    }

    private HighlightColorEnum getHighlightColorEnum() {
        for (HighlightColorEnum e : HighlightColorEnum.values()) {
            if (this.jrbuttonHighlightColor[e.ordinal()].isSelected()) return e;
        }
        return null; // shouldn't happen
    }

    private int getHighlightTransparency() {
        return ((Number)this.jftextHighlightTransparency.getValue()).intValue();
    }

    private int getNumColors() {
        return ((Number)this.jftextNumColors.getValue()).intValue();
    }

    /**
     * show this modal dialog.
     */
    protected void showDialog() {
        if (SwingUtilities.isEventDispatchThread()) {                        showDialogInternal(); }
        else SwingUtilities.invokeLater(new Runnable() { public void run() { showDialogInternal(); } });
    }
    private void showDialogInternal() {
        this.closedByOkButton = false;
        this.setValues(
                this.controller.getWidth(),
                this.controller.getHeight(),
                this.controller.getNumColors(),
                this.controller.getStartPos(),
                this.controller.getGridLines(),
                this.controller.getBoardColorNumbers(),
                this.controller.getHighlightColor(),
                this.controller.getUiColorsNumber(),
                this.controller.getCellSize(),
                this.controller.getLafName(),
                this.controller.getHighlightTransparency(),
                this.controller.isControlPanelEast() );
        this.getRootPane().setDefaultButton(this.buttonOk);
        this.pack();
        this.setLocationRelativeTo(this.mainWindow);
        this.setVisible(true);
    }

    protected void setValues(
            final int width,
            final int height,
            final int numColors,
            final StartPositionEnum spe,
            final GridLinesEnum gle,
            final BoardColorNumbersEnum bcne,
            final HighlightColorEnum hce,
            final int uiColorsNumber,
            final int cellSize,
            final String lafName,
            final int highlightTransparency,
            final boolean isControlPanelEast ) {
        this.doUiPreview = false;
        this.jftextWidth.setValue(Integer.valueOf(width));
        this.jftextHeight.setValue(Integer.valueOf(height));
        this.jftextNumColors.setValue(Integer.valueOf(numColors));
        this.jcomboStartPos.setSelectedIndex(spe.ordinal());
        this.jrbuttonGridLines[gle.ordinal()].setSelected(true);
        this.jrbuttonBoardColorNumbers[bcne.ordinal()].setSelected(true);
        this.jrbuttonHighlightColor[hce.ordinal()].setSelected(true);
        this.jcomboColorSchemes.setSelectedIndex(uiColorsNumber);
        this.jftextCellSize.setValue(Integer.valueOf(cellSize));
        this.jcomboLookAndFeel.setSelectedItem(lafName);
        this.jftextHighlightTransparency.setValue(Integer.valueOf(highlightTransparency));
        this.jrbuttonControlPanelEast[isControlPanelEast ? 1 : 0].setSelected(true);
        this.doUiPreview = true;
        this.userPreviewUiColors();
    }
}
