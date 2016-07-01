/*
 * Copyright © 2013 – 2016 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

public class LabeledProperty<T> {

    public final static String PROPERTY_AB_LABEL = "X-ABLabel";

    public final T property;
    public String label;

    public LabeledProperty(T property) {
        this.property = property;
    }

    public LabeledProperty(T property, String label) {
        this(property);
        this.label = label;
    }

}
