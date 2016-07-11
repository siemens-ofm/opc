package org.openscada.opc.lib.hda;

import org.openscada.opc.lib.da.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * wang-tao.wt@siemens.com
 * Created by wangtao on 2016/7/11.
 */
public class HdaItem {
    private static Logger logger = LoggerFactory.getLogger ( Item.class );

    private int serverHandle = 0;

    private int clientHandle  = 0;

    HdaItem (final int serverHandle, final int clientHandle)
    {
        super ();
        this.serverHandle = serverHandle;
        this.clientHandle = clientHandle;
    }

    public int getServerHandle ()
    {
        return this.serverHandle;
    }

    public int getClientHandle ()
    {
        return this.clientHandle;
    }
}
