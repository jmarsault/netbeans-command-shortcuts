package org.netbeans.shortcuts;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.KeyStroke;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

final public class ShortcutSettings {

    public static final String PROP_SHORTCUT_NAMES = "shortcutList";
    public static final String PROP_COMMAND_LIST = "commandList";
    private static final String SHORTCUT_DELIMITER = "§";
    private List<String> shortcutNames = new ArrayList<String>();
    private LinkedList<String> commands = new LinkedList<String>();
    private PropertyChangeSupport propertySupport;
    private static ShortcutSettings shortcutSettings = null;
    private Method getKeyStroke;
    private Class UtilsClass;

    /**
     * Creates a new instance of Settings
     */
    private ShortcutSettings() {
        shortcutNames.addAll(decodePatterns(getPreferences().get(PROP_SHORTCUT_NAMES, "")));
        commands.addAll(decodePatterns(getPreferences().get(PROP_COMMAND_LIST, "")));
        ClassLoader l = Thread.currentThread().getContextClassLoader();
        try {
            UtilsClass = l.loadClass("org.netbeans.modules.options.keymap.Utils");
            getKeyStroke = UtilsClass.getDeclaredMethod("getKeyStroke", new Class<?>[]{String.class});
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        getKeyStroke.setAccessible(true);

    }

    public static synchronized ShortcutSettings getDefault() {
        if (shortcutSettings == null) {
            shortcutSettings = new ShortcutSettings();
        }
        return shortcutSettings;
    }

    public Map<String, String> getShortcuts() {
        Map<String, String> newShortcuts = new LinkedHashMap<String, String>();
        for (String name : shortcutNames) {
            String cmd = getPreferences().get(name, "");
            newShortcuts.put(name, cmd);
        }
        return Collections.unmodifiableMap(newShortcuts);
    }

    public void setShortcuts(Map<String, String> newShortcuts) {
        shortcutNames.clear();
        shortcutNames.addAll(newShortcuts.keySet());
        getPreferences().put(PROP_SHORTCUT_NAMES, encodeCommands(newShortcuts.keySet()));
        for (Map.Entry<String, String> shortcut : newShortcuts.entrySet()) {
            getPreferences().put(shortcut.getKey(), shortcut.getValue());
        }

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
        Map<String, String> shortcuts = new LinkedHashMap<String, String>();
        for (String name : shortcutNames) {
            String cmd = getPreferences().get("keystroke_" + name, "");
            shortcuts.put(name, cmd);
        }
        return Collections.unmodifiableMap(shortcuts);
    }

    public String getKeystrokeShortcut(String name) {
        return getPreferences().get("keystroke_" + name, "");
    }

    public KeyStroke getKeyStroke(String name) {
        String shortcut = getPreferences().get("keystroke_" + name, "");
        KeyStroke keystroke = null;
        try {
            keystroke = (KeyStroke) getKeyStroke.invoke(UtilsClass, new Object[]{shortcut});
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
        return keystroke;
    }

    public void setKeystrokeShortcuts(Map<String, String> keystrokes) {
        for (Map.Entry<String, String> keystroke : keystrokes.entrySet()) {
            getPreferences().put("keystroke_" + keystroke.getKey(), keystroke.getValue());
        }

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

        Collection<String> patterns = new ArrayList<String>();

        while (st.hasMoreTokens()) {
            String im = st.nextToken();
            patterns.add(im);
        }

        return patterns;
    }

    private static String encodeCommands(Collection<String> patterns) {
        StringBuilder sb = new StringBuilder();

        for (String p : patterns) {
            sb.append(p);
            sb.append(SHORTCUT_DELIMITER);
        }

        return sb.toString();
    }
}
