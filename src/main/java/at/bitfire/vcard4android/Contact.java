/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import java.util.LinkedList;
import java.util.List;

import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Email;
import ezvcard.property.Telephone;
import lombok.Getter;

public class Contact {

	public static final TelephoneType
			PHONE_TYPE_CALLBACK = TelephoneType.get("x-callback"),
			PHONE_TYPE_COMPANY_MAIN = TelephoneType.get("x-company_main"),
			PHONE_TYPE_RADIO = TelephoneType.get("x-radio"),
			PHONE_TYPE_ASSISTANT = TelephoneType.get("X-assistant"),
			PHONE_TYPE_MMS = TelephoneType.get("x-mms");

	public static final EmailType EMAIL_TYPE_MOBILE = EmailType.get("x-mobile");


	public String displayName, nickName;
	public String prefix, givenName, middleName, familyName, suffix;
	public String phoneticGivenName, phoneticMiddleName, phoneticFamilyName;

	public String notes;

	@Getter private List<Telephone> phoneNumbers = new LinkedList<>();
	@Getter private List<Email> emails = new LinkedList<>();

}
