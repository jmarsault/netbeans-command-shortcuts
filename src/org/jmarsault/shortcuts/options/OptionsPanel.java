package org.jmarsault.shortcuts.options;

import org.jmarsault.shortcuts.ShortcutSettings;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.text.BadLocationException;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.netbeans.core.options.keymap.api.*;

class OptionsPanel extends javax.swing.JPanel {

    private boolean changed = false;
    private ShortcutSettings settings;

    public OptionsPanel() {
        initComponents();
        tblCommand.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCommand.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                if ("tableCellEditor".equals(evt.getPropertyName())) {
                    final boolean wasChanged = changed;
                    final TableCellEditor editor = tblCommand.getCellEditor();
                    if (editor != null) {
                        editor.addCellEditorListener(new CellEditorListener() {
                            @Override
                            public void editingStopped(ChangeEvent e) {
                                editor.removeCellEditorListener(this);
                                changed = true;
                                firePropertyChange(OptionsPanelController.PROP_CHANGED, new Boolean(wasChanged), Boolean.TRUE);
                                firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
                            }

                            @Override
                            public void editingCanceled(ChangeEvent e) {
                                editor.removeCellEditorListener(this);
                            }
                        });
                    }
                }
            }
        });
        tblCommand.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2
                        && tblCommand.getSelectedColumn() == 1) {
                    try {
                        showShortcutsDialog();
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }

            }
        });
        tblCommand.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
                commandValueChanged(e);
            }
        });

        edtCommand.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    edtCommand.requestFocusInWindow();
                    e.consume();
                }
            }
        });

        edtCommand.setEnabled(false);
        edtCommand.setEditorKit(JEditorPane.createEditorKitForContentType("text/plain")); //NOI18N
        edtCommand.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                edtCommandDocumentChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                edtCommandDocumentChanged();
            }

            public void changedUpdate(DocumentEvent e) {
                // ignore
            }
        });

        jScrollPane1.getViewport().setOpaque(false);
        enableButtons();
        settings = ShortcutSettings.getDefault();
    }

    private void enableButtons() {
        int selIndex = tblCommand.getSelectedRow();
        btnRemove.setEnabled(selIndex >= 0);
        btnSetShortcut.setEnabled(selIndex >= 0);
    }

    private void commandValueChanged(ListSelectionEvent e) {
        int index = tblCommand.getSelectedRow();

        if (index < 0 || index >= tblCommand.getRowCount()) {
            edtCommand.setText("");
            edtCommand.setEnabled(false);

        } else {
            if (tblCommand.getModel().getValueAt(index, 1) != null) {
                edtCommand.setText(tblCommand.getModel().getValueAt(index, 1).toString()); //NOI18N
            } else {
                edtCommand.setText("");
            }
            edtCommand.getCaret().setDot(0);
            edtCommand.setEnabled(true);
        }
    }

    private void edtCommandDocumentChanged() {
        int index = tblCommand.getSelectedRow();
        if (index >= 0) {
            tblCommand.getModel().setValueAt(edtCommand.getText(), index, 1);
        }
    }

    void update() {
        Map<String, String> shortcuts = settings.getShortcuts();
        tblCommand.setModel(createModel(shortcuts));
        tblCommand.removeColumn(tblCommand.getColumnModel().getColumn(1));

        lbl_error.setText("");
        changed = false;
    }

    void applyChanges() {
        DefaultTableModel model = (DefaultTableModel) tblCommand.getModel();

        Map<String, String> commands = new LinkedHashMap<String, String>();
        Map<String, String> shortcuts = new LinkedHashMap<String, String>();

        for (int i = 0; i < model.getRowCount(); i++) {
            String name = (String) model.getValueAt(i, 0);
            String command = (String) model.getValueAt(i, 1);
            String shortcut = (String) model.getValueAt(i, 2);
            commands.put(name, command != null ? command : "");
            shortcuts.put(name, shortcut != null ? shortcut : "");
        }
        settings.setShortcuts(commands);

        settings.setKeystrokeShortcuts(shortcuts);
    }

    boolean isDataValid() {
        DefaultTableModel model = (DefaultTableModel) tblCommand.getModel();
        List<String> names = new ArrayList<String>();

        for (int i = 0; i < model.getRowCount(); i++) {
            String name = (String) model.getValueAt(i, 0);
            if (name == null || name.isEmpty()) {
                lbl_error.setText(NbBundle.getMessage(OptionsPanel.class, "errorMissingName"));
                return false;
            } else if (names.contains(name)) {
                lbl_error.setText(NbBundle.getMessage(OptionsPanel.class, "errorDuplicateName", name));
                return false;
            } else {
                names.add(name);
            }
        }
        lbl_error.setText("");
        return true;
    }

    boolean isChanged() {
        return changed;


    }

    private DefaultTableModel createModel(Map<String, String> shortcuts) {

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.lblHeaderName.text"),
                    NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.lblHeaderCommand.text"),
                    NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.lblHeaderShortcut.text")}, shortcuts.size()); //NOI18N

        int row = 0;

        for (Map.Entry<String, String> shortcut : shortcuts.entrySet()) {
            model.setValueAt(shortcut.getKey(), row, 0);
            model.setValueAt(shortcut.getValue(), row, 1);
            model.setValueAt(settings.getKeystrokeShortcut(shortcut.getKey()), row++, 2);
        }

        return model;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblCommand = new MyTable();
        lblCommandLine = new javax.swing.JLabel();
        btnAdd = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        lbl_error = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        edtCommand = new javax.swing.JEditorPane();
        btnSetShortcut = new javax.swing.JButton();
        lbl_examples = new javax.swing.JLabel();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        jScrollPane1.setOpaque(false);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(450, 402));

        tblCommand.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tblCommand.setOpaque(false);
        jScrollPane1.setViewportView(tblCommand);

        lblCommandLine.setLabelFor(tblCommand);
        org.openide.awt.Mnemonics.setLocalizedText(lblCommandLine, org.openide.util.NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.lblHeaderCommand.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btnAdd, org.openide.util.NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.btnAdd.text")); // NOI18N
        btnAdd.setActionCommand(org.openide.util.NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.btnAdd.text")); // NOI18N
        btnAdd.setMaximumSize(new java.awt.Dimension(200, 23));
        btnAdd.setMinimumSize(new java.awt.Dimension(50, 23));
        btnAdd.setPreferredSize(new java.awt.Dimension(50, 23));
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btnRemove, org.openide.util.NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.btnRemove.text")); // NOI18N
        btnRemove.setMaximumSize(new java.awt.Dimension(200, 23));
        btnRemove.setMinimumSize(new java.awt.Dimension(50, 23));
        btnRemove.setPreferredSize(new java.awt.Dimension(200, 23));
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });

        lbl_error.setForeground(Color.red);
        lbl_error.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jScrollPane4.setPreferredSize(new java.awt.Dimension(108, 50));

        edtCommand.setMinimumSize(new java.awt.Dimension(50, 20));
        jScrollPane4.setViewportView(edtCommand);

        org.openide.awt.Mnemonics.setLocalizedText(btnSetShortcut, org.openide.util.NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.btnSetShortcut.text")); // NOI18N
        btnSetShortcut.setMaximumSize(new java.awt.Dimension(200, 23));
        btnSetShortcut.setMinimumSize(new java.awt.Dimension(50, 23));
        btnSetShortcut.setPreferredSize(new java.awt.Dimension(200, 23));
        btnSetShortcut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetShortcutActionPerformed(evt);
            }
        });

        lbl_examples.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_examples, org.openide.util.NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.lblExamples.text")); // NOI18N
        lbl_examples.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                lbl_examplesMouseEntered(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                lbl_examplesMousePressed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_error, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_examples, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(lblCommandLine)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnSetShortcut, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(btnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(btnAdd, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSetShortcut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblCommandLine, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(11, 11, 11)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lbl_examples, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_error, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        btnAdd.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.btnAdd.text")); // NOI18N
        btnRemove.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OptionsPanel.class, "OptionsPanel.btnRemove.text")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
    TableCellEditor editor = tblCommand.getCellEditor();
    if (null != editor) {
        editor.cancelCellEditing();
    }

    boolean wasValid = isDataValid();

    int selRow = tblCommand.getSelectedRow();
    if (selRow < 0) {
        return;
    }
    DefaultTableModel model = (DefaultTableModel) tblCommand.getModel();
    model.removeRow(selRow);
    if (selRow > model.getRowCount() - 1) {
        selRow--;
    }
    if (selRow >= 0) {
        tblCommand.getSelectionModel().setSelectionInterval(selRow, selRow);
    }

//    boolean wasChanged = changed;
//    changed = true;
//    firePropertyChange(OptionsPanelController.PROP_CHANGED, new Boolean(wasChanged), Boolean.TRUE);
//
    firePropertyChange(OptionsPanelController.PROP_VALID, new Boolean(wasValid), new Boolean(isDataValid()));
}//GEN-LAST:event_btnRemoveActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        DefaultTableModel model = (DefaultTableModel) tblCommand.getModel();

        model.addRow(new Object[]{}); //NOI18N
        tblCommand.getSelectionModel().setSelectionInterval(model.getRowCount() - 1, model.getRowCount() - 1);
        tblCommand.setColumnSelectionInterval(0, 0);
        try {
            edtCommand.getDocument().remove(0, edtCommand.getDocument().getLength());
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        tblCommand.editCellAt(model.getRowCount() - 1, 0);
        tblCommand.requestFocus();
}//GEN-LAST:event_btnAddActionPerformed

    private void btnSetShortcutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetShortcutActionPerformed
        try {
            showShortcutsDialog();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_btnSetShortcutActionPerformed

    private void lbl_examplesMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_examplesMouseEntered
        evt.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }//GEN-LAST:event_lbl_examplesMouseEntered

    private void lbl_examplesMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_examplesMousePressed
        URL u = null;
        try {
            u = new URL(NbBundle.getMessage(OptionsPanel.class, "EXAMPLE_URL"));
        } catch (MalformedURLException exc) {
            Exceptions.printStackTrace(exc);
        }
        if (u != null) {
            org.openide.awt.HtmlBrowser.URLDisplayer.getDefault().showURL(u);
        }
    }//GEN-LAST:event_lbl_examplesMousePressed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnRemove;
    private javax.swing.JButton btnSetShortcut;
    private javax.swing.JEditorPane edtCommand;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JLabel lblCommandLine;
    private javax.swing.JLabel lbl_error;
    private javax.swing.JLabel lbl_examples;
    private javax.swing.JTable tblCommand;
    // End of variables declaration//GEN-END:variables

    public void showShortcutsDialog() throws IllegalAccessException,
            IllegalArgumentException,
            NoSuchMethodException, ClassNotFoundException {

        ShortcutsFinder shortcutsFinder = Lookup.getDefault().lookup(ShortcutsFinder.class);
        assert shortcutsFinder != null : "Can't find ShortcutsFinder"; //NOI18N
        String shortcut = shortcutsFinder.showShortcutsDialog();

        if (shortcut != null) {
            DefaultTableModel model = (DefaultTableModel) tblCommand.getModel();
            model.setValueAt(shortcut, tblCommand.getSelectedRow(), 2);
        }
    }

    private class MyTable extends JTable {

        public boolean isCellEditable(int row, int column) {
            return column != 1;
        }
    }
}
