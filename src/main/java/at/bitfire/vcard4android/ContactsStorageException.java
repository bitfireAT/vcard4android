/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

public class ContactsStorageException extends Exception {

	public ContactsStorageException(String message) {
		super(message);
	}

	public ContactsStorageException(String message, Throwable ex) {
		super(message, ex);
	}

}
