/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

public class AndroidContactFactory {

	public static final AndroidContactFactory INSTANCE = new AndroidContactFactory();

	public AndroidContact newInstance(AndroidAddressBook addressBook, long id, String fileName, String eTag) {
		return new AndroidContact(addressBook, id, fileName, eTag);
	}

    public 	AndroidContact newInstance(AndroidAddressBook addressBook, Contact contact, String fileName, String eTag) {
		return new AndroidContact(addressBook, contact, fileName, eTag);
	}

    public AndroidContact[] newArray(int size) {
		return new AndroidContact[size];
	}

}
