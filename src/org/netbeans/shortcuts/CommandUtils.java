package org.netbeans.shortcuts;

import java.io.*;
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

public class CommandUtils {

    private static final String UNIX_SEPARATOR = "/";
    private static final String WINDOWS_ESCAPED_SEPARATOR = "\\";

    public static void exec(String command) {
        StringWriter infos = new StringWriter();
        StringWriter errors = new StringWriter();

        try {
            command = parse(command);
            Runtime runtime = Runtime.getRuntime();

            Process process = runtime.exec(command);

            ProcessStream outputStream = new ProcessStream(process.getInputStream(), new PrintWriter(infos, true));
            ProcessStream errorStream = new ProcessStream(process.getErrorStream(), new PrintWriter(errors, true));
            outputStream.start();
            errorStream.start();

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static String parse(String cmd) {
        DataObject dobj = Utilities.actionsGlobalContext().lookup(DataObject.class);
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
                        String folder = normalize(fo.getParent().getPath());
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
            }

        }

        if (Utilities.isWindows()) {
            cmd = "cmd.exe /C " + cmd;
        }

        return cmd;
    }

    private static String normalize(String path) {
        if (Utilities.isWindows()) {
            path = path.replace(UNIX_SEPARATOR, WINDOWS_ESCAPED_SEPARATOR);
        }
        return path;
    }

    static FileObject getFileFromNode(Node node) {
        FileObject fo = (FileObject) node.getLookup().lookup(FileObject.class);
        if (fo == null) {
            Project p = (Project) node.getLookup().lookup(Project.class);
            if (p != null) {
                return p.getProjectDirectory();
            }

            DataObject dobj = (DataObject) node.getCookie(DataObject.class);
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
                br = new BufferedReader(new InputStreamReader(in));
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
