package org.nakedobjects.viewer.skylark;

import org.nakedobjects.object.Naked;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedObjectRuntimeException;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.control.Consent;
import org.nakedobjects.object.control.Veto;
import org.nakedobjects.utility.DebugString;


public class CollectionElement extends ObjectContent {
    private final NakedObject object;

    public CollectionElement(NakedObject object) {
        this.object = object;
    }

    public boolean isObject() {
        return true;
    }
    
    public Consent canClear() {
        return Veto.DEFAULT;
    }

    public Consent canSet(NakedObject dragSource) {
        return Veto.DEFAULT;
    }

    public void clear() {
        throw new NakedObjectRuntimeException("Invalid call");
    }

    public void debugDetails(DebugString debug) {
        debug.appendln(4, "element", object);
    }
    
   /* public String getIconName() {
        return object.getIconName();
    }*/
    
/*    public Image getIconPicture(int iconHeight) {
        NakedObjectSpecification specification = object.getObject() instanceof NakedClass ? ((NakedClass) object.getObject()).forObjectType() : object
                .getSpecification();
        return  ImageFactory.getInstance().loadIcon(specification, "", iconHeight);
    }
*/
    public String getName() {
        return "";
    }

    public NakedObject getObject() {
        return object;
    }

    public Naked getNaked() {
        return object;
    }


    public NakedObjectSpecification getSpecification() {
        return object.getSpecification();
    }

    public boolean isTransient() {
        return ! object.isPersistent();
    }
 
    public void setObject(NakedObject object) {
        throw new NakedObjectRuntimeException("Invalid call");
    }
    
    public String title() {
        return object.titleString();
    }
    
    public String windowTitle() {
        return getSpecification().getShortName();
    }

    public String toString() {
        return "" + object;
    }
}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2005 Naked Objects Group
 * Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address
 * of Naked Objects Group is Kingsway House, 123 Goldworth Road, Woking GU21
 * 1NR, UK).
 */