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

import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JISystem;

/**
 * Class representing the unsigned c++ short.
 * 
 * @since 1.15(b)
 */
public final class JIUnsignedShort implements IJIUnsigned
{

    private final Integer shortValue;

    JIUnsignedShort ( final Integer shortValue )
    {
        if ( shortValue == null || shortValue.intValue () < 0 )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_UNSIGNED_NEGATIVE ) );
        }
        this.shortValue = shortValue;
    }

    @Override
    public int getType ()
    {
        return JIFlags.FLAG_REPRESENTATION_UNSIGNED_SHORT;
    }

    @Override
    public Number getValue ()
    {
        return this.shortValue;
    }

}
