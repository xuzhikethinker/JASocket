/*
 * Copyright 2012 Bill La Forge
 *
 * This file is part of AgileWiki and is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (LGPL) as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * or navigate to the following url http://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Note however that only Scala, Java and JavaScript files are being covered by LGPL.
 * All other files are covered by the Common Public License (CPL).
 * A copy of this license is also included and can be
 * found as well at http://www.opensource.org/licenses/cpl1.0.txt
 */
package org.agilewiki.jasocket.sshd;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jasocket.server.Server;
import org.agilewiki.jasocket.server.ServerCommand;
import org.apache.mina.util.ConcurrentHashSet;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.util.Iterator;

public class SSHServer extends Server {
    private int sshPort;
    private SshServer sshd;
    public ConcurrentHashSet<JASShell> shells = new ConcurrentHashSet<JASShell>();

    @Override
    protected String serverName() {
        return "sshServer";
    }

    @Override
    protected void startServer(PrintJid out, RP rp) throws Exception {
        sshPort = sshPort();
        out.println("ssh port: " + sshPort);
        sshd = SshServer.setUpDefaultServer();
        setAuthenticator();
        sshd.setPort(sshPort);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
        setShellFactory();
        sshd.start();
        super.startServer(out, rp);
    }

    @Override
    public void close() {
        try {
            if (sshd != null)
                sshd.stop(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        super.close();
    }

    protected int sshPort() throws Exception {
        return node().clusterPort() + 1;
    }

    protected void setAuthenticator() {
        sshd.setPasswordAuthenticator(new DummyPasswordAuthenticator(this));
    }

    protected void setShellFactory() {
        sshd.setShellFactory(new JASShellFactory(this, node()));
    }

    protected void registerWriteCommand() {
        registerServerCommand(new ServerCommand("write", "Displays a message on a user's console") {
            @Override
            public void eval(String args, PrintJid out, RP<PrintJid> rp) throws Exception {
                if (shells.size() == 0) {
                    out.println("no operators present");
                } else {
                    int i = args.indexOf(' ');
                    if (i == -1) {
                        out.println("no message is present, only operator name " + args);
                    } else {
                        String name = args.substring(0, i);
                        String msg = args.substring(i + 1);
                        Iterator<JASShell> it = shells.iterator();
                        boolean found = false;
                        while (it.hasNext()) {
                            JASShell sh = it.next();
                            if (sh.getOperatorName().equals(name)) {
                                found = true;
                                sh.notice(": " + msg);
                            }
                        }
                    }
                }
                rp.processResponse(out);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
            node.startup(SSHServer.class, "");
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
