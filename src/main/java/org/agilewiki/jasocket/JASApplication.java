package org.agilewiki.jasocket;

import org.agilewiki.jasocket.node.Node;

public interface JASApplication {
    public void create(Node node) throws Exception;
    public void open() throws Exception;
    public void close();
}
