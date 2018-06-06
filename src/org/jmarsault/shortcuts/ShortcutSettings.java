package org.jmarsault.shortcuts;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.KeyStroke;
import org.openide.util.NbPreferences;
import org.netbeans.core.options.keymap.api.*;

final public class ShortcutSettings {

    public static final String PROP_SHORTCUT_NAMES = "shortcutList";
    public static final String PROP_COMMAND_LIST = "commandList";
    private static final String SHORTCUT_DELIMITER = "§";
    private final List<String> shortcutNames = new ArrayList<>();
    private final LinkedList<String> commands = new LinkedList<>();
    private PropertyChangeSupport propertySupport;
    private static ShortcutSettings shortcutSettings = null;

    /**
     * Creates a new instance of Settings
     */
    private ShortcutSettings() {
        shortcutNames.addAll(decodePatterns(getPreferences().get(PROP_SHORTCUT_NAMES, "")));
        commands.addAll(decodePatterns(getPreferences().get(PROP_COMMAND_LIST, "")));
    }

    public static synchronized ShortcutSettings getDefault() {
        if (shortcutSettings == null) {
            shortcutSettings = new ShortcutSettings();
        }
        return shortcutSettings;
    }

    public Map<String, String> getShortcuts() {
        Map<String, String> newShortcuts = new LinkedHashMap<>();
        shortcutNames.forEach((name) -> {
          String cmd = getPreferences().get(name, "");
          newShortcuts.put(name, cmd);
      });
        return Collections.unmodifiableMap(newShortcuts);
    }

    public void setShortcuts(Map<String, String> newShortcuts) {
        shortcutNames.clear();
        shortcutNames.addAll(newShortcuts.keySet());
        getPreferences().put(PROP_SHORTCUT_NAMES, encodeCommands(newShortcuts.keySet()));
        newShortcuts.entrySet().forEach((shortcut) -> {
          getPreferences().put(shortcut.getKey(), shortcut.getValue());
      });

        if (null == propertySupport) {
            propertySupport = new PropertyChangeSupport(this);
        }
        propertySupport.firePropertyChange(PROP_SHORTCUT_NAMES, null, getShortcuts());
    }

    public void addShortcut(String name, String newCommand) {
        shortcutNames.add(name);
        getPreferences().put(PROP_SHORTCUT_NAMES, encodeCommands(shortcutNames));
        getPreferences().put(name, newCommand);
        if (null == propertySupport) {
            propertySupport = new PropertyChangeSupport(this);
        }
        propertySupport.firePropertyChange(PROP_SHORTCUT_NAMES, null, getShortcuts());
    }

    public void removeShortcut(String name) {
        shortcutNames.remove(name);
        getPreferences().put(PROP_SHORTCUT_NAMES, encodeCommands(shortcutNames));
        getPreferences().remove(name);
        if (null == propertySupport) {
            propertySupport = new PropertyChangeSupport(this);
        }
        propertySupport.firePropertyChange(PROP_SHORTCUT_NAMES, null, getShortcuts());
    }

    public Collection<String> getCommands() {
        return Collections.unmodifiableCollection(commands);
    }

    public void setCommands(Collection<String> newCommands) {
        commands.clear();
        commands.addAll(newCommands);
        getPreferences().put(PROP_COMMAND_LIST, encodeCommands(newCommands));
        if (null == propertySupport) {
            propertySupport = new PropertyChangeSupport(this);
        }
        propertySupport.firePropertyChange(PROP_COMMAND_LIST, null, getCommands());
    }

    public void addCommand(String newCommand) {
        if (!commands.remove(newCommand) && commands.size() >= 10) {
            commands.removeLast();
        }
        commands.addFirst(newCommand);
        getPreferences().put(PROP_COMMAND_LIST, encodeCommands(commands));
        if (null == propertySupport) {
            propertySupport = new PropertyChangeSupport(this);
        }
        propertySupport.firePropertyChange(PROP_COMMAND_LIST, null, getCommands());
    }

    public Map<String, String> getKeystrokeShortcuts() {
        Map<String, String> shortcuts = new LinkedHashMap<>();
        shortcutNames.forEach((name) -> {
          String cmd = getPreferences().get("keystroke_" + name, "");
          shortcuts.put(name, cmd);
      });
        return Collections.unmodifiableMap(shortcuts);
    }

    public String getKeystrokeShortcut(String name) {
        return getPreferences().get("keystroke_" + name, "");
    }

    public KeyStroke getKeyStroke(String name) {
        String shortcut = getPreferences().get("keystroke_" + name, "");
        KeyStroke keystroke = KeyStrokeUtils.getKeyStroke(shortcut);
        return keystroke;
    }

    public void setKeystrokeShortcuts(Map<String, String> keystrokes) {
      keystrokes.entrySet().forEach((keystroke) -> {
        getPreferences().put("keystroke_" + keystroke.getKey(), keystroke.getValue());
      });

        if (null == propertySupport) {
            propertySupport = new PropertyChangeSupport(this);
        }
        propertySupport.firePropertyChange(PROP_SHORTCUT_NAMES, null, getShortcuts());
    }

    public void addKeystrokeShortcut(String name, String keystroke) {
        getPreferences().put("keystroke_" + name, keystroke);
        if (null == propertySupport) {
            propertySupport = new PropertyChangeSupport(this);
        }
        propertySupport.firePropertyChange(PROP_SHORTCUT_NAMES, null, getShortcuts());
    }

    public void removeKeystrokeShortcut(String name) {
        getPreferences().remove("keystroke_" + name);
        if (null == propertySupport) {
            propertySupport = new PropertyChangeSupport(this);
        }
        propertySupport.firePropertyChange(PROP_SHORTCUT_NAMES, null, getShortcuts());
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (null == propertySupport) {
            propertySupport = new PropertyChangeSupport(this);
        }
        propertySupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (null != propertySupport) {
            propertySupport.removePropertyChangeListener(l);
        }
    }

    private Preferences getPreferences() {
        return NbPreferences.forModule(ShortcutSettings.class);
    }

    private static Collection<String> decodePatterns(String encodedPatterns) {
        StringTokenizer st = new StringTokenizer(encodedPatterns, SHORTCUT_DELIMITER, false);

        Collection<String> patterns = new ArrayList<>();

        while (st.hasMoreTokens()) {
            String im = st.nextToken();
            patterns.add(im);
        }

        return patterns;
    }

    private static String encodeCommands(Collection<String> patterns) {
        StringBuilder sb = new StringBuilder();

        patterns.stream().map((p) -> {
          sb.append(p);
        return p;
      }).forEachOrdered((_item) -> {
        sb.append(SHORTCUT_DELIMITER);
      });

        return sb.toString();
    }
}
