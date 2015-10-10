/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

public class AndroidGroupFactory {

	public static final AndroidGroupFactory INSTANCE = new AndroidGroupFactory();

	AndroidGroup newInstance(AndroidAddressBook addressBook, long id) {
		return new AndroidGroup(addressBook, id);
	}

	AndroidGroup newInstance(AndroidAddressBook addressBook, Contact contact) {
		return new AndroidGroup(addressBook, contact);
	}

	AndroidGroup[] newArray(int size) {
		return new AndroidGroup[size];
	}

}
