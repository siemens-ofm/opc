package org.openscada.opc.hda;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.*;
import org.openscada.opc.dcom.common.FILETIME;
import org.openscada.opc.dcom.common.impl.Helper;
import org.openscada.opc.lib.VariantDumper;
import org.openscada.opc.lib.da.ItemState;
import org.openscada.opc.lib.hda.HDAIID;
import org.openscada.opc.lib.hda.OPCHDA_ITEM;
import org.openscada.opc.lib.hda.OPCHDA_TIME;

import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * wang-tao.wt@siemens.com
 * Created by wangtao on 2016/7/8.
 */
public class Test1 {
    private static final String JIPointer = null;

    public static void main(String[] args) throws UnknownHostException, JIException {
        // TODO Auto-generated method stub
        String domainName = "";
        String userName = "opc";
        String password = "opc";
        String hostIP = "localhost";
        //String progId = "Matrikon.OPC.Simulation.1";
        String clsid = "F8582CF2-88FB-11D0-B850-00C0F0104305" ;

        JISession dcomSession= JISession.createSession(domainName,userName,password);
        dcomSession.useSessionSecurity(false);

        //JIComServer comServer = new JIComServer(JIProgId.valueOf(progId), hostIP , dcomSession);
        JIComServer comServer = new JIComServer(JIClsid.valueOf(clsid), hostIP , dcomSession);

        IJIComObject serverInstance = comServer.createInstance();

        IJIComObject serverObject = serverInstance.queryInterface(HDAIID.IOPCHDA_Server);

        int serverhandle = getItemHandles(serverObject, "Random.Int1");

        Calendar c = Calendar.getInstance();
        Date ss = new Date();
        c.setTime(ss);
        c.add(Calendar.MINUTE, -10);

        syncRead(serverObject, c.getTime(), ss, serverhandle);

        JISession.destroySession ( dcomSession );

    }

    public static void syncRead(IJIComObject serverInstance, Date begin, Date end, int serverhandle) throws JIException{

        IJIComObject syncReadObject;
        try {
            syncReadObject = serverInstance.queryInterface(HDAIID.IOPCHDA_SyncRead);
        } catch (JIException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return ;
        }

        JICallBuilder callObject = new JICallBuilder ( true );

        callObject.setOpnum ( 0 );

        FILETIME b = new FILETIME(begin);
        FILETIME e = new FILETIME(end);

        callObject.addInParamAsStruct(new OPCHDA_TIME(false, "" ,b).toStruct(), JIFlags.FLAG_NULL);
        callObject.addInParamAsStruct(new OPCHDA_TIME(false, "" ,e).toStruct(), JIFlags.FLAG_NULL);
        callObject.addInParamAsInt ( 1000, JIFlags.FLAG_NULL );
        callObject.addInParamAsBoolean(true, JIFlags.FLAG_NULL);
        callObject.addInParamAsInt ( 1, JIFlags.FLAG_NULL );

        final Integer[] phServers = new Integer[1];
        phServers[0] = serverhandle;
        callObject.addInParamAsArray ( new JIArray(phServers, true), JIFlags.FLAG_NULL );

        callObject.addOutParamAsObject(OPCHDA_TIME.getStruct(), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(OPCHDA_TIME.getStruct(), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject ( new JIPointer ( new JIArray (OPCHDA_ITEM.getStruct(), null, 1, true ) ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( new JIPointer ( new JIArray ( Integer.class, null, 1, true ) ), JIFlags.FLAG_NULL );

        final Object result[];
        try {
            result = Helper.callRespectSFALSE ( syncReadObject, callObject );
            //result = syncReadObject.call(callObject);
        } catch (JIException ex) {
            // TODO Auto-generated catch block
            System.out.println(ex);
            ex.printStackTrace();
            return;
        }

        JIStruct[] results = (JIStruct[]) ((JIArray) ((JIPointer) result[2]).getReferent()).getArrayInstance();
        Integer[] errorCodes = (Integer[]) ((JIArray) ((JIPointer) result[3]).getReferent()).getArrayInstance();

        System.out.println("length : " + results.length);
        System.out.println("length : " + errorCodes.length);

        for (int i = 0;i < result.length;i++){
            OPCHDA_ITEM item = new OPCHDA_ITEM(results[i]);
            List<ItemState> itemStates = item.getItemStates();
            if (itemStates != null){
                for (ItemState state:itemStates){
                    VariantDumper.dumpValue(state.getValue().getObject());
                    char a = (char)state.getValue().getObject();
                    System.out.println(state.getValue().getObject());
                    System.out.println(state.getTimestamp().getTime());
                    System.out.println(state.getQuality());
                }
            }
        }

        JIStruct first = results[0];

        System.out.println("length : " + first.getMember(2));


        final Integer[] q = (Integer[]) ((JIArray) ((JIPointer) first.getMember(4)).getReferent()).getArrayInstance();



        for(int i=0;i<q.length;i++){
            System.out.println(q[i].shortValue());
        }

//        final JIVariant[] values = (JIVariant[]) ((JIArray) ((JIPointer) first.getMember(5)).getReferent()).getArrayInstance();
//        final Object[] resultss = new Object[values.length];
//        for (int i=0; i < values.length; i++)
//        	System.out.println(values[i].getObject().toString());

    }

    public static int getItemHandles(IJIComObject serverInstance, String itemName){

        JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum (3);

        final JIString[] itemArray = new JIString[1];
        itemArray[0] = new JIString("Random.Int1", JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);
        callObject.addInParamAsInt ( 1, JIFlags.FLAG_NULL );
        callObject.addInParamAsArray ( new JIArray(itemArray, true), JIFlags.FLAG_NULL );

        final Integer[] phClients = new Integer[1];
        phClients[0] = 1;
        callObject.addInParamAsArray ( new JIArray(phClients, true), JIFlags.FLAG_NULL );

        callObject.addOutParamAsObject ( new JIPointer ( new JIArray ( Integer.class, null, 1, true ) ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( new JIPointer ( new JIArray ( Integer.class, null, 1, true ) ), JIFlags.FLAG_NULL );

        final Object result[];
        try {
            result = Helper.callRespectSFALSE ( serverInstance, callObject );
        } catch (JIException e) {
            // TODO Auto-generated catch block
            System.out.println(e);
            e.printStackTrace();
            return 0;
        }

        Integer[] serverhandles = (Integer[]) ( (JIArray) ( (JIPointer)result[0] ).getReferent () ).getArrayInstance ();
        Integer[] errorCodes = (Integer[]) ( (JIArray) ( (JIPointer)result[1] ).getReferent () ).getArrayInstance ();

        System.out.println(serverhandles[0]);
        return serverhandles[0];
    }
}
