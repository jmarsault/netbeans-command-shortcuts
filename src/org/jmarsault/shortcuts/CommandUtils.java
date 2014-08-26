package org.jmarsault.shortcuts;

import java.awt.Component;
import java.io.*;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.text.JTextComponent;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataShadow;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.windows.TopComponent;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.queries.FileEncodingQuery;

public class CommandUtils {

    private static final String UNIX_SEPARATOR = "/";
    private static final String WINDOWS_ESCAPED_SEPARATOR = "\\";

    public static void exec(String name, String command) {
        try {
            String[] commandArray = parse(command);
            // Show something as Outputwindow Caption
            //String name = command.split(" ")[0];
            Runtime runtime = Runtime.getRuntime();
            final Process process = runtime.exec(commandArray);

            Callable processCallable = new Callable() {

                @Override
                public Process call() throws IOException {
                    return process;
                }
            };

            ExecutionDescriptor descriptor = new ExecutionDescriptor().frontWindow(true).controllable(true);

            ExecutionService service = ExecutionService.newService(processCallable, descriptor, name);

            Future task = service.run();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static String[] parse(String cmd) {
        DataObject dobj = Utilities.actionsGlobalContext().lookup(DataObject.class);
        TopComponent activated = TopComponent.getRegistry().getActivated();

        if (dobj == null && TopComponent.getRegistry().getActivated() != null) {

            if (TopComponent.getRegistry().getActivated() != null) {
                Node[] activedNodes = activated.getActivatedNodes();
                if (activedNodes != null && activedNodes.length > 0) {
                    dobj = activedNodes[0].getLookup().lookup(DataObject.class);
                }
            }
        }
        Collection<? extends Node> nodez = Utilities.actionsGlobalContext().lookup(new Lookup.Template<Node>(Node.class)).allInstances();
        Node[] nodes = nodez.toArray(new Node[nodez.size()]);
        Pattern pVar = Pattern.compile("#\\{([a-zA-Z]+[0-9]*)\\}");
        Matcher mVar = pVar.matcher(cmd);

        while (mVar.find()) {

            String var = mVar.group(1);

            if (dobj != null) {

                Pattern pIndex = Pattern.compile("\\d+");
                Matcher mIndex = pIndex.matcher(var);
                String index = "";
                FileObject fo = null;

                if (mIndex.find()) {
                    index = mIndex.group();
                    if (Integer.valueOf(index) <= nodes.length) {
                        fo = getFileFromNode(nodes[Integer.valueOf(index) - 1]);
                    }
                } else {
                    fo = dobj.getPrimaryFile();
                }

                if (fo != null) {
                    if (("file" + index).equalsIgnoreCase(var)) {
                        String file = normalize(fo.getPath());
                        cmd = cmd.replace("#{" + var + "}", file);
                    } else if (("filename" + index).equalsIgnoreCase(var)) {
                        cmd = cmd.replace("#{" + var + "}", fo.getName());
                    } else if (("filenameext" + index).equalsIgnoreCase(var)) {
                        cmd = cmd.replace("#{" + var + "}", fo.getNameExt());
                    } else if (("folder" + index).equalsIgnoreCase(var)) {
                        String folder;
                        if (fo.isFolder()) {
                            folder = normalize(fo.getPath());
                        } else {
                            folder = normalize(fo.getParent().getPath());
                        }
                        cmd = cmd.replace("#{" + var + "}", folder);
                    } else if (("foldername" + index).equalsIgnoreCase(var)) {
                        cmd = cmd.replace("#{" + var + "}", fo.getParent().getName());
                    } else if (("selection" + index).equalsIgnoreCase(var)) {
                        EditorCookie pane = dobj.getCookie(EditorCookie.class);
                        JEditorPane[] panes = pane.getOpenedPanes();
                        if (panes != null && panes.length > 0) {
                            JTextComponent comp = panes[0];
                            String selection = comp.getSelectedText();
                            if (selection != null) {
                                cmd = cmd.replace("#{" + var + "}", selection);
                            } else {
                            }
                        }
                    }
                }
            }

            if ("input".equalsIgnoreCase(var)) {
                NotifyDescriptor.InputLine msg = new NotifyDescriptor.InputLine("Input", "");
                DialogDisplayer.getDefault().notify(msg);
                cmd = cmd.replace("#{" + var + "}", msg.getInputText());
            } else if ("password".equalsIgnoreCase(var)) {
                Object[] message = new Object[2];
                message[0] = "Your password";
                message[1] = new JPasswordField();

                String option[] = {"OK", "Annuler"};

                JOptionPane.showOptionDialog(
                        null,
                        message,
                        "Password",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        option,
                        message[1]);
                cmd = cmd.replace("#{" + var + "}", new String(((JPasswordField) message[1]).getPassword()));

            } else if (("mainproject").equalsIgnoreCase(var)) {
                String projectPath = normalize(OpenProjects.getDefault().getMainProject().getProjectDirectory().getPath());
                cmd = cmd.replace("#{" + var + "}", projectPath);
            } else if (("selection").equalsIgnoreCase(var)) {
                Component[] components = activated.getComponents();
                for (Component component : components) {
                    if (component.getClass().getName().contains("OutputTab")) {
                        try {
                            //Hack
                            ClassLoader l = Thread.currentThread().getContextClassLoader();
                            Class outputTabClass = l.loadClass("org.netbeans.core.output2.OutputTab");
                            Method getOutputPane = outputTabClass.getMethod("getOutputPane");
                            Object abstractOutputPaneClass = (Object) getOutputPane.invoke(component);

                            Method getSelectedText = abstractOutputPaneClass.getClass().getMethod("getSelectedText");
                            String selection = (String) getSelectedText.invoke(abstractOutputPaneClass);

                            if (selection != null) {
                                cmd = cmd.replace("#{" + var + "}", selection);
                            }
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }

                    }
                }
            } else if (("file").equalsIgnoreCase(var)) {
//                    Component[] components = activated.getComponents();
//                    for (Component component : components) {
//                        if (component.getClass().getName().contains("OutputTab")) {
//                            try {
//                                //Hack
//                                ClassLoader l = Thread.currentThread().getContextClassLoader();
//                                Class outputTabClass = l.loadClass("org.netbeans.core.output2.OutputTab");
//                                Method getOutputPane = outputTabClass.getMethod("getOutputPane");
//                                Object abstractOutputPaneClass = (Object) getOutputPane.invoke(component);
//                                
//                                Method getSelectedText = abstractOutputPaneClass.getClass().getMethod("getSelectedText");
//                                String selection = (String) getSelectedText.invoke(abstractOutputPaneClass);
//                                Document doc;
//                                doc.
//                                if (selection != null) {
//                                    cmd = cmd.replace("#{" + var + "}", selection);
//                                }
//                            } catch (Exception ex) {
//                                Exceptions.printStackTrace(ex);
//                            }
//
//                        }
//                    }
            }
        }

        String[] cmdArray = new String[]{cmd};
        if (Utilities.isWindows()) {
            cmdArray = new String[]{"cmd", "/C", cmd};
        }
        else
          if (Utilities.isUnix ()) {
              cmdArray = new String[]{"/bin/sh", "-c", cmd};
          }

        return cmdArray;
    }

    private static String normalize(String path) {
        if (Utilities.isWindows()) {
            path = path.replace(UNIX_SEPARATOR, WINDOWS_ESCAPED_SEPARATOR);
        }
        return path;
    }

    static FileObject getFileFromNode(Node node) {
        FileObject fo = node.getLookup().lookup(FileObject.class);
        if (fo == null) {
            Project p = node.getLookup().lookup(Project.class);
            if (p != null) {
                return p.getProjectDirectory();
            }

            DataObject dobj = node.getCookie(DataObject.class);
            if (dobj instanceof DataShadow) {
                dobj = ((DataShadow) dobj).getOriginal();
            }
            if (dobj != null) {
                fo = dobj.getPrimaryFile();
            }
        }
        return fo;
    }

    public static File getFirstApplicationFile(String cmd) {
        String app;
        Pattern bcRegex = Pattern.compile("^\\\"(.*?)\\\"");
        Matcher bcMatcher = bcRegex.matcher(cmd);
        if (bcMatcher.find()) {
            app = bcMatcher.group(1);
        } else {
            String[] split = cmd.split(" ");
            app = split[0];
        }
        File appFile = new File(app);

        return appFile;
    }

    static class ProcessStream extends Thread {

        private InputStream in;
        private PrintWriter pw;

        ProcessStream(InputStream in, PrintWriter pw) {
            this.in = in;
            this.pw = pw;
        }

        @Override
        public void run() {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(in, FileEncodingQuery.getDefaultEncoding()));
                String line = null;
                while ((line = br.readLine()) != null) {
                    pw.println(line);
                }
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    Exceptions.printStackTrace(e);
                }
            }
        }
    }
}
