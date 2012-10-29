package org.jmarsault.shortcuts;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Map;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.*;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

@ActionID(category = "File",
id = "org.netbeans.shortcuts.ShortcutAction")
@ActionRegistration(displayName = "#CTL_ShortcutAction")
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-R"),
    @ActionReference(path = "Toolbars/Shortcut", position = -100)
})
@Messages({"CTL_ShortcutAction=Execute command", "CTL_ContextMenuAction=Shortcuts"})
public final class ShortcutAction extends AbstractAction implements Presenter.Toolbar, Presenter.Popup, PropertyChangeListener {

    private static final String PREFERRED_ICON_SIZE = "PreferredIconSize";
    private JButton toggleButton;
    private JPopupMenu popup;
    private final FileSystemView fileSystemView;
    private final ShortcutSettings settings;
    private final Icon ICON = new ImageIcon(ImageUtilities.loadImage("org/jmarsault/shortcuts/shortcut24.png"));
    private final Icon ICON_SMALL = new ImageIcon(ImageUtilities.loadImage("org/jmarsault/shortcuts/shortcut16.png"));

    public ShortcutAction() {
        settings = ShortcutSettings.getDefault();
        fileSystemView = FileSystemView.getFileSystemView();
        popup = new JPopupMenu();

        toggleButton = DropDownButtonFactory.createDropDownButton(ICON, popup);
        setToggleButtonIcon();
        toggleButton.addActionListener(this);
        toggleButton.addPropertyChangeListener(PREFERRED_ICON_SIZE, this);
        settings.addPropertyChangeListener(this);
        addItemsTo(popup);
        addKeyStrokesTo(toggleButton);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final RunPanel shortcutPanel = new RunPanel();

        JButton btnExecute = new JButton(NbBundle.getMessage(ShortcutAction.class, "RunPanel.btnRun.text"));
        Mnemonics.setLocalizedText(btnExecute, org.openide.util.NbBundle.getMessage(RunPanel.class, "RunPanel.btnRun.text"));
        btnExecute.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shortcutPanel.executeActionPerformed();
                JOptionPane.getRootFrame().dispose();
            }
        });

        JButton btnCancel = new JButton();
        Mnemonics.setLocalizedText(btnCancel, org.openide.util.NbBundle.getMessage(RunPanel.class, "RunPanel.btnCancel.text"));
        btnCancel.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.getRootFrame().dispose();
            }
        });

        JButton btnSave = new JButton();
        Mnemonics.setLocalizedText(btnSave, org.openide.util.NbBundle.getMessage(RunPanel.class, "RunPanel.btnSave.text"));
        btnSave.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shortcutPanel.saveActionPerformed();
            }
        });

        JButton[] option = {btnExecute, btnSave, btnCancel};

        JOptionPane.showOptionDialog(
                null,
                shortcutPanel,
                NbBundle.getMessage(ShortcutAction.class, "RunPanel.title.text"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                option, null);

    }

    @Override
    public Component getToolbarPresenter() {
        return toggleButton;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        JMenu jMenu = new JMenu(NbBundle.getMessage(ShortcutAction.class, "CTL_ContextMenuAction"));
        addItemsTo(jMenu);
        return jMenu;
    }

    private void addItemsTo(JComponent jComponent) {

        for (final Map.Entry<String, String> shortcut : settings.getShortcuts().entrySet()) {
            File app = CommandUtils.getFirstApplicationFile(shortcut.getValue());
            final JMenuItem menuItem = new JMenuItem();
            menuItem.setText(shortcut.getKey());
            menuItem.setToolTipText(shortcut.getValue());
            menuItem.setIcon(getSystemIcon(app));
            menuItem.setActionCommand(shortcut.getValue());
            menuItem.setAccelerator(settings.getKeyStroke(shortcut.getKey()));
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    JMenuItem menuItem = (JMenuItem) evt.getSource();
                    evt.setSource(menuItem.getActionCommand());
                    btnShortcutActionPerformed(evt);
                }
            });

            jComponent.add(menuItem);
        }
        jComponent.add(new JSeparator());
        JMenuItem menuItem = new JMenuItem(NbBundle.getMessage(ShortcutAction.class, "lbl_option"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                OptionsDisplayer.getDefault().open(OptionsDisplayer.ADVANCED + "/org.netbeans.shortcuts.options.Shortcut");
            }
        });
        jComponent.add(menuItem);
    }

    private void addKeyStrokesTo(JButton btn) {

        for (final Map.Entry<String, String> shortcut : settings.getShortcuts().entrySet()) {

            btn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(settings.getKeyStroke(shortcut.getKey()), shortcut.getValue());
            btn.getActionMap().put(shortcut.getValue(), new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    e.setSource(shortcut.getValue());
                    btnShortcutActionPerformed(e);
                }
            });
        }
    }

    private void btnShortcutActionPerformed(ActionEvent evt) {
        CommandUtils.exec((String) evt.getSource());
    }

    private Icon getSystemIcon(File app) {
        int size = 16;
        Icon icon = fileSystemView.getSystemIcon(app);

        if (icon != null) {
            Image img = ImageUtilities.icon2Image(icon).getScaledInstance(size, size, Image.SCALE_SMOOTH);
            ImageUtilities.image2Icon(img);
        }
        return icon;

    }

    private void removeShortcuts() {
        toggleButton.removeAll();
        popup.removeAll();
    }

    private void setToggleButtonIcon() {
        if (ToolbarPool.getDefault().getPreferredIconSize() == 16) {
            toggleButton.setIcon(ICON_SMALL);
        } else {
            toggleButton.setIcon(ICON);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (PREFERRED_ICON_SIZE.equals(evt.getPropertyName())) {
            setToggleButtonIcon();
        } else {
            removeShortcuts();
            addKeyStrokesTo(toggleButton);
            addItemsTo(popup);
        }
    }
}
