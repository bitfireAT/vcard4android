/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import ezvcard.property.VCardProperty

data class LabeledProperty<out T: VCardProperty> @JvmOverloads constructor(
        val property: T,
        var label: String? = null
)