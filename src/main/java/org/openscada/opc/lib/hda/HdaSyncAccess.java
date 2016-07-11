package org.openscada.opc.lib.hda;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.*;
import org.openscada.opc.dcom.common.FILETIME;
import org.openscada.opc.dcom.common.impl.Helper;
import org.openscada.opc.lib.da.ItemState;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * wang-tao.wt@siemens.com
 * Created by wangtao on 2016/7/11.
 */
public class HdaSyncAccess extends HdaAccessBase {


    public HdaSyncAccess(HdaServer hdaServer) throws JIException {
        super(hdaServer, HDAIID.IOPCHDA_SyncRead);
    }

    @Override
    public OPCHDA_ITEM readRaw(String itemName, Date begin, Date end) throws JIException {
        if (!this.hdaServer.isAddedItem(itemName)){
            this.addItem(itemName);
        }
        Integer [] phServers = new Integer[1];
        phServers[0] = this.hdaServer.getServerHandle(itemName);
        return read(phServers, begin, end).get(0);
    }

    @Override
    public List<OPCHDA_ITEM> readRaws(List<String> itemName, Date begin, Date end) {
        return null;
    }

    private List<OPCHDA_ITEM> read(Integer[] phServers, Date begin, Date end) throws JIException{
        JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum ( 0 );
        callObject.addInParamAsStruct(new OPCHDA_TIME(false, "" ,new FILETIME(begin)).toStruct(), JIFlags.FLAG_NULL);
        callObject.addInParamAsStruct(new OPCHDA_TIME(false, "" ,new FILETIME(end)).toStruct(), JIFlags.FLAG_NULL);
        callObject.addInParamAsInt ( 100, JIFlags.FLAG_NULL );
        callObject.addInParamAsBoolean(true, JIFlags.FLAG_NULL);
        callObject.addInParamAsInt ( phServers.length, JIFlags.FLAG_NULL );

        callObject.addInParamAsArray ( new JIArray(phServers, true), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject(OPCHDA_TIME.getStruct(), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(OPCHDA_TIME.getStruct(), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject ( new JIPointer( new JIArray (OPCHDA_ITEM.getStruct(), null, 1, true ) ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( new JIPointer ( new JIArray ( Integer.class, null, 1, true ) ), JIFlags.FLAG_NULL );

        final Object result[];
        try {
            result = Helper.callRespectSFALSE ( this.getObject(), callObject );
        } catch (JIException e) {
            System.out.println(e);
            e.printStackTrace();
            return null;
        }

        JIStruct[] results = (JIStruct[]) ((JIArray) ((JIPointer) result[2]).getReferent()).getArrayInstance();
        Integer[] errorCodes = (Integer[]) ((JIArray) ((JIPointer) result[3]).getReferent()).getArrayInstance();

        System.out.println("length : " + results.length);
        System.out.println("length : " + errorCodes.length);

        List<OPCHDA_ITEM> list = new ArrayList<>();

        for (int i = 0;i < results.length;i++){
            OPCHDA_ITEM item = new OPCHDA_ITEM(results[i], errorCodes[i]);
            list.add(item);
        }
        return list;
    }
}
