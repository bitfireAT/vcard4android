package at.bitfire.vcard4android.datarow

import android.content.ContentProviderClient
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.RawContacts
import at.bitfire.vcard4android.AndroidContact
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.datavalues.EmailHandler
import at.bitfire.vcard4android.datavalues.PhotoHandler
import java.util.*
import java.util.logging.Level

class ContactProcessor(
    val provider: ContentProviderClient?
) {

    private val dataRowHandlers = mutableMapOf<String, MutableList<DataRowHandler>>()
    private val defaultDataRowHandlers = arrayOf(
        EmailHandler,
        EventHandler,
        ImHandler,
        NicknameHandler,
        NoteHandler,
        OrganizationHandler,
        PhoneNumberHandler,
        PhotoHandler(provider),
        RelationHandler,
        SipAddressHandler,
        StructuredNameHandler,
        StructuredPostalHandler,
        WebsiteHandler
    )

    private val dataRowBuilderFactories = LinkedList<DataRowBuilder.Factory<*>>()
    private val defaultDataRowBuilderFactories = arrayOf(
        EmailBuilder.Factory,
        EventBuilder.Factory,
        ImBuilder.Factory,
        NicknameBuilder.Factory,
        NoteBuilder.Factory,
        OrganizationBuilder.Factory,
        PhoneBuilder.Factory,
        PhotoBuilder.Factory,
        RelationBuilder.Factory,
        SipAddressBuilder.Factory,
        StructuredNameBuilder.Factory,
        StructuredPostalBuilder.Factory,
        WebsiteBuilder.Factory
    )


    init {
        for (handler in defaultDataRowHandlers)
            registerHandler(handler)
        for (factory in defaultDataRowBuilderFactories)
            registerBuilderFactory(factory)
    }


    fun registerHandler(handler: DataRowHandler) {
        val mimeType = handler.forMimeType()
        val handlers = dataRowHandlers[mimeType] ?: run {
            val newList = mutableListOf<DataRowHandler>()
            dataRowHandlers[mimeType] = newList
            newList
        }

        handlers += handler
    }

    fun registerBuilderFactory(factory: DataRowBuilder.Factory<*>) {
        dataRowBuilderFactories += factory
    }


    fun handleRawContact(values: ContentValues, contact: Contact) {
        contact.uid = values.getAsString(AndroidContact.COLUMN_UID)
    }

    fun handleDataRow(values: ContentValues, contact: Contact) {
        val mimeType = values.getAsString(RawContacts.Data.MIMETYPE)

        val handlers = dataRowHandlers[mimeType].orEmpty()
        if (handlers.isNotEmpty())
            for (handler in handlers)
                handler.handle(values, contact)
        else
            Constants.log.log(Level.WARNING, "No registered handler for $mimeType", values)
    }


    fun insertDataRows(dataRowUri: Uri, rawContactId: Long?, contact: Contact, batch: BatchOperation) {
        for (factory in dataRowBuilderFactories) {
            val builder = factory.newInstance(dataRowUri, rawContactId, contact)
            batch.enqueueAll(builder.build())
        }
    }


    fun builderMimeTypes(): Set<String> {
        val mimeTypes = mutableSetOf<String>()
        for (factory in dataRowBuilderFactories)
            mimeTypes += factory.mimeType()
        return mimeTypes
    }

}