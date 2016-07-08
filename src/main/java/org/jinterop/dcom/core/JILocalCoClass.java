/**j-Interop (Pure Java implementation of DCOM protocol)
 * Copyright (C) 2006  Vikram Roopchand
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * Though a sincere effort has been made to deliver a professional,
 * quality product,the library itself is distributed WITHOUT ANY WARRANTY;
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110, USA
 */

package org.jinterop.dcom.core;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ndr.NetworkDataRepresentation;

import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.core.UUID;

/**
 * <p>
 * Represents a Java <code>COCLASS</code>.
 * <p>
 * <i>Please refer to MSInternetExplorer, Test_ITestServer2_Impl,
 * SampleTestServer and MSShell examples for more details on how to use this
 * class.</i><br>
 * 
 * @since 2.0 (formerly JIJavaCoClass)
 */
public final class JILocalCoClass implements Serializable
{
    private final static Logger logger = LoggerFactory.getLogger ( JILocalCoClass.class );

    private static final long serialVersionUID = 5542223845228327383L;

    private static Random randomGen = new Random ( Double.doubleToRawLongBits ( Math.random () ) );

    private final int identifier;

    private WeakReference interfacePointer = null;

    private boolean isAlreadyExported = false;

    private byte[] objectID = null;

    private JILocalInterfaceDefinition interfaceDefinition = null;

    private static final String IID_IDispatch = "00020400-0000-0000-c000-000000000046";

    private final ArrayList listOfSupportedInterfaces = new ArrayList ();

    private final ArrayList listOfSupportedEventInterfaces = new ArrayList ();

    private final HashMap mapOfIIDsToInterfaceDefinitions = new HashMap ();

    private JISession session = null;

    private boolean realIID = false;

    static
    {

    }

    private final Map ipidVsIID = new HashMap ();// will use this to identify which IID is being talked about

    //if it is IDispatch then delegate to it's invoke.

    private final Map IIDvsIpid = new HashMap ();// will use this to identify which IPID is being talked about

    private void init ( final JILocalInterfaceDefinition interfaceDefinition, final Class clazz, final Object instance, final boolean realIID )
    {
        this.listOfSupportedInterfaces.add ( IID_IDispatch.toUpperCase () ); //IDispatch
        this.listOfSupportedInterfaces.add ( "00000131-0000-0000-C000-000000000046" ); //IRemUnknown
        this.interfaceDefinition = interfaceDefinition;
        interfaceDefinition.clazz = clazz;
        interfaceDefinition.instance = instance;
        this.listOfSupportedInterfaces.add ( interfaceDefinition.getInterfaceIdentifier ().toUpperCase () );
        this.mapOfIIDsToInterfaceDefinitions.put ( interfaceDefinition.getInterfaceIdentifier ().toUpperCase (), interfaceDefinition );
        this.realIID = realIID;
    }

    /**
     * Creates a local class instance. The framework will try to create a
     * instance of the <code>clazz</code> using <code>Class.newInstance</code>.
     * Make sure that <code>clazz</code> has a visible <code>null</code>
     * constructor.
     * 
     * @param interfaceDefinition
     *            implementing structurally the definition of the COM callback
     *            interface.
     * @param clazz
     *            <code>class</code> to instantiate for serving requests from
     *            COM client. Must implement
     *            the <code>interfaceDefinition</code> fully.
     * @throws IllegalArgumentException
     *             if <code>interfaceDefinition</code> or <code>clazz</code> are
     *             <code>null</code>.
     */
    public JILocalCoClass ( final JILocalInterfaceDefinition interfaceDefinition, final Class clazz )
    {
        if ( interfaceDefinition == null || clazz == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_COM_RUNTIME_INVALID_CONTAINER_INFO ) );
        }
        this.identifier = clazz.hashCode () ^ new Object ().hashCode () ^ randomGen.nextInt ();
        init ( interfaceDefinition, clazz, null, false );
    }

    /**
     * Refer {@link #JILocalCoClass(JILocalInterfaceDefinition, Class)}.
     * 
     * @param interfaceDefinition
     *            implementing structurally the definition of the COM callback
     *            interface.
     * @param clazz
     *            <code>class</code> to instantiate for serving requests from
     *            COM client. Must implement
     *            the <code>interfaceDefinition</code> fully.
     * @param useInterfaceDefinitionIID
     *            <code>true</code> if the <code>IID</code> of
     *            <code>interfaceDefinition</code should be used as to create
     *            the local COM Object. Use this when a reference other than
     *            <code>IUnknown*</code> is required.
     *            For all
     *            {@link JIObjectFactory#attachEventHandler(IJIComObject, String, IJIComObject)}
     *            operations this should be set to <code>false</code> since the
     *            <code>IConnectionPoint::Advise</code> method takes in a
     *            <code>IUnknown*</code> reference.
     * @throws IllegalArgumentException
     *             if <code>interfaceDefinition</code> or <code>clazz</code> are
     *             <code>null</code>.
     */
    public JILocalCoClass ( final JILocalInterfaceDefinition interfaceDefinition, final Class clazz, final boolean useInterfaceDefinitionIID )
    {
        if ( interfaceDefinition == null || clazz == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_COM_RUNTIME_INVALID_CONTAINER_INFO ) );
        }
        this.identifier = clazz.hashCode () ^ new Object ().hashCode () ^ randomGen.nextInt ();
        init ( interfaceDefinition, clazz, null, useInterfaceDefinitionIID );
    }

    /**
     * Creates a local class instance.
     * 
     * @param interfaceDefinition
     *            implementing structurally the definition of the COM callback
     *            interface.
     * @param instance
     *            instance for serving requests from COM client. Must implement
     *            the <code>interfaceDefinition</code> fully.
     * @throws IllegalArgumentException
     *             if <code>interfaceDefinition</code> or <code>instance</code>
     *             are <code>null</code>.
     */
    public JILocalCoClass ( final JILocalInterfaceDefinition interfaceDefinition, final Object instance )
    {
        if ( interfaceDefinition == null || instance == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_COM_RUNTIME_INVALID_CONTAINER_INFO ) );
        }
        this.identifier = instance.hashCode () ^ new Object ().hashCode () ^ randomGen.nextInt ();
        init ( interfaceDefinition, null, instance, false );
    }

    /**
     * Creates a local class instance.
     * 
     * @param interfaceDefinition
     *            implementing structurally the definition of the COM callback
     *            interface.
     * @param instance
     *            instance for serving requests from COM client. Must implement
     *            the <code>interfaceDefinition</code> fully.
     * @param useInterfaceDefinitionIID
     *            <code>true</code> if the <code>IID</code> of
     *            <code>interfaceDefinition</code should be used as to create
     *            the local COM Object. Use this when a reference other than
     *            <code>IUnknown*</code> is required.
     *            For all
     *            {@link JIObjectFactory#attachEventHandler(IJIComObject, String, IJIComObject)}
     *            operations this should be set to <code>false</code> since the
     *            <code>IConnectionPoint::Advise</code> method takes in a
     *            <code>IUnknown*</code> reference.
     * @throws IllegalArgumentException
     *             if <code>interfaceDefinition</code> or <code>instance</code>
     *             are <code>null</code>.
     */
    public JILocalCoClass ( final JILocalInterfaceDefinition interfaceDefinition, final Object instance, final boolean useInterfaceDefinitionIID )
    {
        if ( interfaceDefinition == null || instance == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_COM_RUNTIME_INVALID_CONTAINER_INFO ) );
        }
        this.identifier = instance.hashCode () ^ new Object ().hashCode () ^ randomGen.nextInt ();
        init ( interfaceDefinition, null, instance, useInterfaceDefinitionIID );
    }

    /**
     * Sets the interface identifiers (<code>IID</code>s) of the event
     * interfaces this class would support. This in case the same
     * <code>clazz</code> or <code>instance</code> is implementing more than one
     * <code>IID</code>.
     * 
     * @param listOfIIDs
     * @see #JILocalCoClass(JILocalInterfaceDefinition, Class)
     * @see #JILocalCoClass(JILocalInterfaceDefinition, Object)
     */
    public void setSupportedEventInterfaces ( final List listOfIIDs )
    {
        if ( listOfIIDs != null )
        {
            for ( int i = 0; i < listOfIIDs.size (); i++ )
            {
                final String s = ( (String)listOfIIDs.get ( i ) ).toUpperCase ();
                this.listOfSupportedInterfaces.add ( s );
                this.listOfSupportedEventInterfaces.add ( s );
                this.mapOfIIDsToInterfaceDefinitions.put ( s, this.interfaceDefinition );
            }

        }
    }

    /**
     * Add another interface definition and it's supporting object instance.
     * 
     * @param interfaceDefinition
     *            implementing structurally the definition of the COM callback
     *            interface.
     * @param instance
     *            instance for serving requests from COM client. Must implement
     *            the <code>interfaceDefinition</code> fully.
     * @throws IllegalArgumentException
     *             if <code>interfaceDefinition</code> or <code>instance</code>
     *             are <code>null</code>.
     */
    public void addInterfaceDefinition ( final JILocalInterfaceDefinition interfaceDefinition, final Object instance )
    {
        if ( interfaceDefinition == null || instance == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_COM_RUNTIME_INVALID_CONTAINER_INFO ) );
        }
        interfaceDefinition.instance = instance;
        final String s = interfaceDefinition.getInterfaceIdentifier ().toUpperCase ();
        this.listOfSupportedInterfaces.add ( s );
        this.listOfSupportedEventInterfaces.add ( s );
        this.mapOfIIDsToInterfaceDefinitions.put ( s, interfaceDefinition );
    }

    /**
     * Add another interface definition and it's class. Make sure that this
     * class has a default constructor,
     * so that instantiation using <i>reflection</i> can take place.
     * 
     * @param interfaceDefinition
     *            implementing structurally the definition of the COM callback
     *            interface.
     * @param clazz
     *            instance for serving requests from COM client. Must implement
     *            the <code>interfaceDefinition</code> fully.
     * @throws IllegalArgumentException
     *             if <code>interfaceDefinition</code> or <code>clazz</code> are
     *             <code>null</code>.
     */
    public void addInterfaceDefinition ( final JILocalInterfaceDefinition interfaceDefinition, final Class clazz )
    {
        if ( interfaceDefinition == null || clazz == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_COM_RUNTIME_INVALID_CONTAINER_INFO ) );
        }
        interfaceDefinition.clazz = clazz;
        final String s = interfaceDefinition.getInterfaceIdentifier ().toUpperCase ();
        this.listOfSupportedInterfaces.add ( s );
        this.listOfSupportedEventInterfaces.add ( s );
        this.mapOfIIDsToInterfaceDefinitions.put ( s, interfaceDefinition );
    }

    /**
     * Returns the instance representing the interface definition. <br>
     * 
     * @return
     * @see #JILocalCoClass(JILocalInterfaceDefinition, Object)
     */
    public Object getServerInstance ()
    {
        return this.interfaceDefinition.instance;
    }

    /**
     * Returns the actual class representing the interface definition. <br>
     * 
     * @return
     * @see #JILocalCoClass(JILocalInterfaceDefinition, Class)
     */
    public Class getServerClass ()
    {
        return this.interfaceDefinition.clazz;
    }

    //	public boolean isDispatchSupported()
    //	{
    //		return isDispatchSupported;
    //	}

    //called from com runtime.
    /**
     * @exclude
     */
    void setObjectId ( final byte[] objectId )
    {
        this.objectID = objectId;
    }

    /**
     * @exclude
     */
    void setAssociatedInterfacePointer ( final JIInterfacePointer interfacePointer )
    {
        this.isAlreadyExported = true;
        this.interfacePointer = new WeakReference ( interfacePointer );
        final String ipid = interfacePointer.getIPID ().toUpperCase ();
        final String iid = interfacePointer.getIID ().toUpperCase ();
        this.IIDvsIpid.put ( iid, ipid );
        this.ipidVsIID.put ( ipid, iid );
    }

    /**
     * @exclude
     */
    boolean isAssociatedReferenceAlive ()
    {
        return this.interfacePointer == null ? false : this.interfacePointer.get () == null ? false : true;
    }

    boolean isAlreadyExported ()
    {
        return this.isAlreadyExported;
    }

    /**
     * @exclude
     */
    byte[] getObjectId ()
    {
        return this.objectID;
    }

    /**
     * @exclude
     * @param iid
     * @return
     */
    boolean isPresent ( String iid )
    {
        iid = iid.toUpperCase ();
        return this.listOfSupportedInterfaces.contains ( iid );
    }

    /**
     * @exclude
     * @param uniqueIID
     * @param IPID
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    //advances the index...it cannot be reversed.
    synchronized boolean exportInstance ( final String uniqueIID, String IPID ) throws InstantiationException, IllegalAccessException
    {
        //Object retval = null;
        IPID = IPID.toUpperCase ();

        if ( !isPresent ( uniqueIID ) )//not supported IID.
        {
            return false;
        }

        this.IIDvsIpid.put ( uniqueIID.toUpperCase (), IPID );
        this.ipidVsIID.put ( IPID, uniqueIID.toUpperCase () );
        return true;
    }

    /**
     * Returns the interface identifier of this COCLASS. <br>
     * 
     * @return
     * @see #JILocalCoClass(JILocalInterfaceDefinition, Class)
     * @see #JILocalCoClass(JILocalInterfaceDefinition, Object)
     * @see JILocalInterfaceDefinition#getInterfaceIdentifier()
     */
    public String getCoClassIID ()
    {
        return this.interfaceDefinition.getInterfaceIdentifier ();
    }

    /**
     * @exclude
     * @param IPID
     * @param Opnum
     * @param inparams
     * @return
     * @throws JIException
     */
    //This will invoke the API via reflection and return the results of the call back to the
    //actual COM object. This API is to be invoked via the RemUnknown Object
    Object[] invokeMethod ( String IPID, int Opnum, final NetworkDataRepresentation ndr ) throws JIException
    {
        IPID = IPID.toUpperCase ();
        //somehow identify the method from the Opnum
        //this will come from the IDL.

        Object retVal = null;//will be an array.

        final String iid = (String)this.ipidVsIID.get ( IPID );
        if ( iid == null )
        {
            throw new JIException ( JIErrorCodes.RPC_E_INVALID_OBJECT );
        }

        JILocalInterfaceDefinition interfaceDefinitionOfClass = (JILocalInterfaceDefinition)this.mapOfIIDsToInterfaceDefinitions.get ( iid );
        interfaceDefinitionOfClass = interfaceDefinitionOfClass == null ? this.interfaceDefinition : interfaceDefinitionOfClass;

        JILocalMethodDescriptor methodDescriptor = null;
        boolean execute = false;
        Object[] params = null;

        //that means the calls will come as IUnknown + IDispatch op numbers...0,1,2 & 3,4,5,6
        //from 7th (inclusive) onwards are the actual COM servers calls
        //now check for dispinterface and take a call...
        //if dispinterface is supported then all calls will come with base of 6 {0,1,2 & 3,4,5,6}
        //i.e 6th will be invoke and 7th(inclusive) onwards will be standard api calls.
        //if not supported than it will be base 2 {0,1,2} i.e real method calls will start from 3(inclusive) onwards.
        boolean isStandardCall = true;
        if ( this.interfaceDefinition.isDispInterface () )
        {
            isStandardCall = false;
            switch ( Opnum )
            {
                case 3: //getTypeInfoCount
                    //not supported
                    retVal = new Object[1];
                    ( (Object[])retVal )[0] = new Integer ( 0 ); //not supported
                    break;
                case 4: //getTypeInfo
                    throw new JIException ( JIErrorCodes.E_NOTIMPL );
                case 5: //getIDOfNames

                    JILocalParamsDescriptor paramObject = new JILocalParamsDescriptor ();

                    paramObject.addInParamAsType ( UUID.class, JIFlags.FLAG_NULL );
                    paramObject.addInParamAsObject ( new JIArray ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR ), null, 1, true ), JIFlags.FLAG_NULL );
                    paramObject.addInParamAsType ( Integer.class, JIFlags.FLAG_NULL );
                    paramObject.addInParamAsType ( Integer.class, JIFlags.FLAG_NULL );

                    //now read and then send the result back.
                    JIArray array = (JIArray)paramObject.read ( ndr )[1];

                    final Object[] arrayObj = (Object[])array.getArrayInstance ();
                    final Integer[] dispIds = new Integer[arrayObj.length];
                    //get the first member of the Array , which is the APINAME and send the retVal with it's dispId
                    final JIString apiName = (JIString)arrayObj[0];
                    JILocalMethodDescriptor info = interfaceDefinitionOfClass.getMethodDescriptor ( apiName.getString () );
                    if ( info == null )
                    {
                        dispIds[0] = new Integer ( JIErrorCodes.DISP_E_UNKNOWNNAME );
                    }
                    else
                    {
                        dispIds[0] = new Integer ( info.getMethodNum () );
                    }

                    //rest are all 0,1,2...parameters
                    for ( int i = 1; i < arrayObj.length; i++ )
                    {
                        dispIds[i] = new Integer ( i - 1 );
                    }

                    final JIArray results = new JIArray ( dispIds );

                    retVal = new Object[1];
                    ( (Object[])retVal )[0] = results;

                    break;
                case 6: //invoke of IDispatch

                    paramObject = new JILocalParamsDescriptor ();
                    paramObject.setSession ( this.session );
                    paramObject.addInParamAsType ( Integer.class, JIFlags.FLAG_NULL );
                    paramObject.addInParamAsType ( UUID.class, JIFlags.FLAG_NULL );
                    paramObject.addInParamAsType ( Integer.class, JIFlags.FLAG_NULL );
                    paramObject.addInParamAsType ( Integer.class, JIFlags.FLAG_NULL );

                    JIStruct dispParams = new JIStruct ();
                    dispParams.addMember ( new JIPointer ( new JIArray ( JIVariant.class, null, 1, true ) ) );
                    dispParams.addMember ( new JIPointer ( new JIArray ( Integer.class, null, 1, true ) ) );
                    dispParams.addMember ( Integer.class );
                    dispParams.addMember ( Integer.class );

                    paramObject.addInParamAsObject ( dispParams, JIFlags.FLAG_REPRESENTATION_IDISPATCH_INVOKE );
                    paramObject.addInParamAsType ( Integer.class, JIFlags.FLAG_NULL );
                    paramObject.addInParamAsObject ( new JIArray ( Integer.class, null, 1, true ), JIFlags.FLAG_NULL );
                    paramObject.addInParamAsObject ( new JIArray ( JIVariant.class, null, 1, true ), JIFlags.FLAG_NULL );

                    final Object[] retresults = paramObject.read ( ndr );
                    //named params not supported
                    final int dispId = ( (Integer)retresults[0] ).intValue ();

                    info = interfaceDefinitionOfClass.getMethodDescriptorForDispId ( dispId );
                    if ( info == null )
                    {
                        logger.error ( "MethodDescriptor not found for DispId :- {}", dispId );

                        throw new JIException ( JIErrorCodes.DISP_E_MEMBERNOTFOUND );
                    }

                    dispParams = (JIStruct)retresults[4];
                    final JIPointer ptrToParamsArray = (JIPointer)dispParams.getMember ( 0 );

                    params = new Object[0];
                    if ( !ptrToParamsArray.isNull () )
                    {
                        //form the real array
                        array = (JIArray)ptrToParamsArray.getReferent ();
                        final Object[] variants = (Object[])array.getArrayInstance ();
                        params = new Object[variants.length];
                        for ( int i = 0; i < variants.length; i++ )
                        {
                            params[i] = ( (JIVariant)variants[i] ).getObject ();
                        }
                    }

                    if ( ( (Integer)retresults[5] ).intValue () != 0 )
                    {
                        //now replace the params at index from the index array.
                        array = (JIArray)retresults[6];
                        final Integer[] indexs = (Integer[])array.getArrayInstance ();
                        array = (JIArray)retresults[7];
                        final JIVariant[] variants = (JIVariant[])array.getArrayInstance ();
                        for ( int i = 0; i < indexs.length; i++ )
                        {
                            params[indexs[i].intValue ()] = variants[i];
                        }

                    }

                    //now to reverse this array of params.
                    final int halflength = params.length / 2;
                    for ( int i = 0; i < halflength; i++ )
                    {
                        final Object t = params[i];
                        params[i] = params[params.length - 1 - i];
                        params[params.length - 1 - i] = t;
                    }

                    methodDescriptor = info;
                    execute = true;
                    break;
                default: //others are normal API calls ...Opnum - 6 is there real Opnum. 0,1,2 and 3,4,5,6
                    isStandardCall = true;
                    Opnum = Opnum - 4; //adjust for only IDispatch(3,4,5,6) , IUnknown(0,1,2) will get adjusted below.
                    logger.info ( "Standard call came: Opnum is {}", Opnum );

            }
        }

        if ( isStandardCall )
        {
            methodDescriptor = interfaceDefinitionOfClass.getMethodDescriptor ( Opnum - 3 ); //adjust for IUnknown
            if ( methodDescriptor == null )
            {
                throw new JIException ( JIErrorCodes.RPC_S_PROCNUM_OUT_OF_RANGE );
            }
            methodDescriptor.getParameterObject ().setSession ( this.session );
            params = methodDescriptor.getParameterObject ().read ( ndr );
            execute = true;
        }

        if ( execute )
        {
            //JILocalInterfaceDefinition interfaceDefinitionOfCall = interfaceDefinition;
            final Class calleeClazz = interfaceDefinitionOfClass.instance == null ? interfaceDefinitionOfClass.clazz : interfaceDefinitionOfClass.instance.getClass ();
            Method method = null;
            try
            {
                logger.info ( "methodDescriptor: {}", methodDescriptor.getMethodName () );
                method = calleeClazz.getDeclaredMethod ( methodDescriptor.getMethodName (), methodDescriptor.getInparametersAsClass () );
                final Object calleeInstance = interfaceDefinitionOfClass.instance == null ? calleeClazz.newInstance () : interfaceDefinitionOfClass.instance;
                logger.info ( "Call Back Method to be executed: {} , to be executed on {}", method, calleeInstance );
                final Object result = method.invoke ( calleeInstance, params );

                if ( result == null )
                {
                    retVal = null;
                }
                else if ( ! ( result instanceof Object[] ) )
                {
                    retVal = new Object[1];
                    ( (Object[])retVal )[0] = result;
                }
                else
                {
                    retVal = result;
                }

            }
            catch ( final IllegalArgumentException e )
            {
                logger.warn ( "invokeMethod", e );
                throw new JIException ( JIErrorCodes.E_INVALIDARG, e );
            }
            catch ( final IllegalAccessException e )
            {
                logger.warn ( "invokeMethod", e );
                throw new JIException ( JIErrorCodes.ERROR_ACCESS_DENIED, e );
            }
            catch ( final InvocationTargetException e )
            {
                logger.warn ( "invokeMethod", e );
                throw new JIException ( JIErrorCodes.E_UNEXPECTED, e );
            }
            catch ( final SecurityException e )
            {
                logger.warn ( "invokeMethod", e );
                throw new JIException ( JIErrorCodes.ERROR_ACCESS_DENIED, e );
            }
            catch ( final NoSuchMethodException e )
            {
                logger.warn ( "invokeMethod", e );
                throw new JIException ( JIErrorCodes.RPC_S_PROCNUM_OUT_OF_RANGE, e );
            }
            catch ( final InstantiationException e )
            {
                logger.warn ( "invokeMethod", e );
                throw new JIException ( JIErrorCodes.E_UNEXPECTED, e );
            }

        }

        return (Object[])retVal;
    }

    /**
     * Returns the primary interfaceDefinition. <br>
     * 
     * @return
     * @see #JILocalCoClass(JILocalInterfaceDefinition, Class)
     * @see #JILocalCoClass(JILocalInterfaceDefinition, Object)
     */
    public JILocalInterfaceDefinition getInterfaceDefinition ()
    {
        return this.interfaceDefinition;
    }

    /**
     * @exclude
     */
    @Override
    public boolean equals ( final Object target )
    {
        if ( target == null || ! ( target instanceof JILocalCoClass ) )
        {
            return false;
        }

        return this.identifier == ( (JILocalCoClass)target ).identifier;
    }

    /**
     * @exclude
     */
    @Override
    public int hashCode ()
    {
        return this.identifier;
    }

    /**
     * Returns the interface definition based on the IID of the interface.
     * 
     * @return <code>null</code> if no interface definition matching the
     *         <code>IID</code> has been found.
     */
    public JILocalInterfaceDefinition getInterfaceDefinition ( final String IID )
    {
        return (JILocalInterfaceDefinition)this.mapOfIIDsToInterfaceDefinitions.get ( IID.toUpperCase () );
    }

    /**
     * @exclude
     * @param IPID
     * @return
     */
    JILocalInterfaceDefinition getInterfaceDefinitionFromIPID ( final String IPID )
    {
        return (JILocalInterfaceDefinition)this.mapOfIIDsToInterfaceDefinitions.get ( this.ipidVsIID.get ( IPID.toUpperCase () ) );
    }

    /**
     * @exclude
     */
    String getIpidFromIID ( final String uniqueIID )
    {
        return (String)this.IIDvsIpid.get ( uniqueIID.toUpperCase () );
    }

    /**
     * @param uniqueIID
     * @return
     */
    String getIIDFromIpid ( final String ipid )
    {
        return (String)this.ipidVsIID.get ( ipid.toUpperCase () );
    }

    /**
     * <p>
     * Returns <code>true</code> if the primary interface definition represents
     * a real <code>IID</code> .
     * 
     * @return
     */
    //	 The bind-auth3 and all are then all done as per this <code>IID</code> and not IUnknown.
    public boolean isCoClassUnderRealIID ()
    {
        return this.realIID;
    }

    /**
     * Associate the Session with this CoClass. Called by the framework.
     * 
     * @exclude
     * @param session
     */
    void setSession ( final JISession session )
    {
        this.session = session;
    }

    /**
     * Returns the session associated with this CoClass.
     * 
     * @return
     */
    JISession getSession ()
    {
        return this.session;
    }

    List getSupportedInterfaces ()
    {
        return this.listOfSupportedInterfaces;
    }
}
