package org.jmarsault.shortcuts.options;

import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

@OptionsPanelController.SubRegistration(location = "Advanced",
id = "org.jmarsault.shortcuts.options.Shortcut",
displayName = "#AdvancedOption_DisplayName_Shortcut",
keywords = "#AdvancedOption_Keywords_Shortcuts",
keywordsCategory = "Advanced/Shortcuts")
@org.openide.util.NbBundle.Messages({"AdvancedOption_DisplayName_Shortcut=Shortcuts", "AdvancedOption_Keywords_Shortcuts=Shortcut"})
public class OptionsController extends OptionsPanelController {

    private OptionsPanel optionsPanel;

    @Override
    public void update() {
        getShortcutPanel().update();
    }

    @Override
    public void applyChanges() {
        if (isValid()) {
            getShortcutPanel().applyChanges();
        }
    }

    @Override
    public void cancel() {
        //do nothing
    }

    @Override
    public boolean isValid() {
        return getShortcutPanel().isDataValid();
    }

    @Override
    public boolean isChanged() {
        return getShortcutPanel().isChanged();
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getShortcutPanel();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("org.jmarsault.shortcuts.options.OptionsController");
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        getShortcutPanel().addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        getShortcutPanel().removePropertyChangeListener(l);
    }

    private OptionsPanel getShortcutPanel() {
        if (null == optionsPanel) {
            optionsPanel = new OptionsPanel();
        }
        return optionsPanel;
    }
}
