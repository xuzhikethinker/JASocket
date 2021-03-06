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
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.cluster.GetAgentChannel;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jid.Jid;

public class ToAgent extends CommandStringAgent {
    private EvalAgent evalAgent;
    private AgentChannel agentChannel;

    @Override
    protected void process(final RP<PrintJid> rp) throws Exception {
        String address = getArgString();
        String argsString = "";
        int p = address.indexOf(' ');
        if (p > -1) {
            argsString = address.substring(p + 1).trim();
            address = address.substring(0, p).trim();
        }
        if (address.length() == 0) {
            println("missing channel or server name");
            rp.processResponse(out);
            return;
        }
        evalAgent = (EvalAgent) JAFactory.newActor(
                this,
                JASocketFactories.EVAL_FACTORY,
                getMailbox(),
                agentChannelManager());
        evalAgent.configure(getOperatorName(), getId(), argsString);
        if (isLocalAddress(address)) {
            StartAgent.req.send(this, evalAgent, (RP) rp);
            return;
        }
        final String a = address;
        (new GetAgentChannel(address)).send(this, agentChannelManager(), new RP<AgentChannel>() {
            @Override
            public void processResponse(AgentChannel agentChannel) throws Exception {
                if (agentChannel == null) {
                    println("not an open channel: " + a);
                    rp.processResponse(out);
                    return;
                }
                ToAgent.this.agentChannel = agentChannel;
                ShipAgent shipAgent = new ShipAgent(evalAgent);
                shipAgent.send(ToAgent.this, agentChannel, new RP<Jid>() {
                    @Override
                    public void processResponse(Jid response) throws Exception {
                        rp.processResponse((PrintJid) response);
                    }
                });
            }
        });
    }

    public void userInterrupt() throws Exception {
        long requestId = evalAgent.getRequestId();
        if (requestId == -1) {
            UserInterrupt.req.sendEvent(this, evalAgent);
            return;
        }
        UserInterruptAgent userInterruptAgent = (UserInterruptAgent) JAFactory.newActor(
                this,
                JASocketFactories.USER_INTERRUPT_AGENT_FACTORY,
                getMailbox(),
                agentChannelManager());
        userInterruptAgent.configure(requestId);
        ShipAgent shipAgent = new ShipAgent(userInterruptAgent);
        shipAgent.sendEvent(this, agentChannel);
    }
}
