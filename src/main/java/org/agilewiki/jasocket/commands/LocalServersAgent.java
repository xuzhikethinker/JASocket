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
package org.agilewiki.jasocket.commands;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.cluster.GetLocalServers;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.server.Server;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import java.util.Iterator;
import java.util.TreeMap;

public class LocalServersAgent extends CommandAgent {
    @Override
    public void process(final RP<PrintJid> rp) throws Exception {
        GetLocalServers.req.send(this, agentChannelManager(), new RP<TreeMap<String, Server>>() {
            @Override
            public void processResponse(TreeMap<String, Server> response) throws Exception {
                Iterator<String> it = response.keySet().iterator();
                while (it.hasNext()) {
                    String name = it.next();
                    Server server = response.get(name);
                    out.println(name + " " +
                            server.getOperatorName() + " " +
                            ISOPeriodFormat.standard().
                                    print(new Period(System.currentTimeMillis() - server.startTime)) + " " +
                            server.startupArgs());
                }
                rp.processResponse(out);
            }
        });
    }
}
