package org.openscada.opc.lib.hda;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.*;
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
import org.openscada.opc.dcom.common.impl.Helper;
import org.openscada.opc.dcom.da.OPCITEMDEF;
import org.openscada.opc.dcom.da.OPCITEMRESULT;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.*;

/**
 * wang-tao.wt@siemens.com
 * Created by wangtao on 2016/7/11.
 */
public class HdaServer {
    private static Logger logger = LoggerFactory.getLogger ( HdaServer.class );

    private final ConnectionInformation connectionInformation;

    private JISession session = null;

    private JIComServer comServer;

    private IJIComObject serverInstance;

    private IJIComObject hdaServer;

    private Integer clientHander = 0;

    private Map<String, HdaItem> itemMap = new HashMap<>();

    public HdaServer ( final ConnectionInformation connectionInformation)
    {
        super ();
        this.connectionInformation = connectionInformation;
    }

    protected IJIComObject queryInterface(String iid) throws JIException {
        return this.serverInstance.queryInterface(iid);
    }

    public synchronized void connect() throws AlreadyConnectedException , UnknownHostException, JIException {
        if (isConnected()){
            throw new AlreadyConnectedException();
        }

        this.session = JISession.createSession ( this.connectionInformation.getDomain (), this.connectionInformation.getUser (), this.connectionInformation.getPassword () );
        this.comServer = new JIComServer ( JIClsid.valueOf ( this.connectionInformation.getClsid () ), this.connectionInformation.getHost (), this.session );
        this.serverInstance = this.comServer.createInstance();
        this.hdaServer  = this.serverInstance.queryInterface(HDAIID.IOPCHDA_Server);
        logger.info("Hda server connected");
    }

    public synchronized  void disconnect() throws JIException{
        if(isConnected()){
            JISession.destroySession(this.session);
            this.session = null;
        }
        logger.info("Hda server disconnected");
    }

    public boolean isConnected(){
        return session != null;
    }

    private void getItemHandles(JIString[] items, Integer[] phClients) throws JIException{
        JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum (3);

        callObject.addInParamAsInt ( items.length, JIFlags.FLAG_NULL );
        callObject.addInParamAsArray ( new JIArray(items, true), JIFlags.FLAG_NULL );
        callObject.addInParamAsArray ( new JIArray(phClients, true), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( new JIPointer ( new JIArray ( Integer.class, null, 1, true ) ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( new JIPointer ( new JIArray ( Integer.class, null, 1, true ) ), JIFlags.FLAG_NULL );

        final Object result[] = Helper.callRespectSFALSE ( this.hdaServer, callObject );

        Integer[] serverhandles = (Integer[]) ( (JIArray) ( (JIPointer)result[0] ).getReferent () ).getArrayInstance ();
        Integer[] errorCodes = (Integer[]) ( (JIArray) ( (JIPointer)result[1] ).getReferent () ).getArrayInstance ();

        for (int i = 0; i < items.length; i++){
            //// TODO: 2016/7/11 TO handle errorCodes, 判断add的个数是否等于返回的server，抛出错误的itemNames
            if (errorCodes[i] == 0) {
                this.itemMap.put(items[i].getString(), new HdaItem(serverhandles[i], phClients[i]));
            }else{

            }
        }
        return ;
    }

    protected synchronized void addItem(String itemName) throws JIException{
        if(this.isAddedItem(itemName)){
            return;
        }
        JIString[] strings = new JIString[1];
        strings[0] = new JIString(itemName, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);
        final Integer[] phClients = new Integer[1];
        phClients[0] = getClientHandle();
        this.getItemHandles(strings, phClients);
    }

    protected synchronized void addItems(Set<String> itemNames) throws JIException{
        List<JIString> strings = new ArrayList<>();
        List<Integer> clients = new ArrayList<>();
        for (String s : itemNames) {
            if(this.isAddedItem(s)){
                continue;
            }
            strings.add(new JIString(s, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR));
            clients.add(getClientHandle());
        }
        this.getItemHandles(strings.toArray(new JIString[strings.size()]), clients.toArray(new Integer[clients.size()]) );
    }

    protected int getServerHandle(String itemName){
        HdaItem hdaItem = itemMap.get(itemName);
        if (hdaItem == null){
            return -1;
        }
        return hdaItem.getServerHandle();
    }

    protected synchronized int getClientHandle(){
        return clientHander++;
    }

    protected boolean isAddedItem(String itemName){
        return itemMap.containsKey(itemName);
    }
}
