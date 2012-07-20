package com.heroku;

import com.sun.jna.Native;
import hudson.FilePath;
import hudson.Functions;
import hudson.Util;
import hudson.org.apache.tools.tar.TarInputStream;
import hudson.remoting.RemoteInputStream;
import hudson.remoting.VirtualChannel;
import hudson.util.IOException2;
import hudson.util.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.tar.TarEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static hudson.util.jna.GNUCLibrary.LIBC;

/**
 * Unfortunately hudson.FilePath#readFromTar does not work when untarring read-only files and overwriting is required.
 * This implementation forces a delete of existing read-only files before copying from the archive. This is less than
 * ideal, but it works. Also, hudson.FilePath is final, so having to do a lot of duplication here for a vert small change :(
 *
 * @author Ryan Brainard
 */
class OverwritableUntar {

    public static void untar(InputStream src, FilePath dest, final FilePath.TarCompression compression) throws IOException, InterruptedException {
        try {
            final InputStream in = new RemoteInputStream(src);
            dest.act(new FilePath.FileCallable<Void>() {
                public Void invoke(File dir, VirtualChannel channel) throws IOException {
                    readFromTar("input stream", dir, compression.extract(in));
                    return null;
                }

                private static final long serialVersionUID = 1L;
            });
        } finally {
            IOUtils.closeQuietly(src);
        }
    }

    private static void readFromTar(String name, File baseDir, InputStream in) throws IOException {
        TarInputStream t = new TarInputStream(in);
        try {
            TarEntry te;
            while ((te = t.getNextEntry()) != null) {
                File f = new File(baseDir, te.getName());
                if (te.isDirectory()) {
                    f.mkdirs();
                } else {
                    File parent = f.getParentFile();
                    if (parent != null) parent.mkdirs();

                    // force delete before copy to a read-only file
                    if (f.exists() && !f.canWrite()) {
                        Util.deleteFile(f);
                    }

                    IOUtils.copy(t, f);

                    f.setLastModified(te.getModTime().getTime());
                    int mode = te.getMode() & 0777;

                    if (mode != 0 && !Functions.isWindows()) // be defensive
                        _chmod(f, mode);
                }
            }
        } catch (IOException e) {
            throw new IOException2("Failed to extract " + name, e);
        } finally {
            t.close();
        }
    }

    private static void _chmod(File f, int mask) throws IOException {
        if (Functions.isWindows()) return; // noop

        try {
            if (LIBC.chmod(f.getAbsolutePath(), mask) != 0) {
                throw new IOException("Failed to chmod " + f + " : " + LIBC.strerror(Native.getLastError()));
            }
        } catch (NoClassDefFoundError e) {  // cf. https://groups.google.com/group/hudson-dev/browse_thread/thread/6d16c3e8ea0dbc9?hl=fr
            _chmodAnt(f, mask);
        } catch (UnsatisfiedLinkError e2) { // HUDSON-8155: use Ant's chmod task on non-GNU C systems
            _chmodAnt(f, mask);
        }
    }

    private static void _chmodAnt(File f, int mask) {
        Chmod chmodTask = new Chmod();
        chmodTask.setProject(new Project());
        chmodTask.setFile(f);
        chmodTask.setPerm(Integer.toOctalString(mask));
        chmodTask.execute();
    }
}
