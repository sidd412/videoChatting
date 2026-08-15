package com.videoChatting.echat.utils

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.videoChatting.echat.data.remote.model.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContactUtils {

    suspend fun getDeviceContacts(context: Context): List<DeviceContact> = withContext(Dispatchers.IO) {
        val contactList = mutableListOf<DeviceContact>()
        val seenNumbers = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Friend" else "Friend"
                val rawNumber = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                
                // Normalize phone number (remove spaces, hyphens, brackets)
                val cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "").trim()

                if (cleanNumber.isNotEmpty() && !seenNumbers.contains(cleanNumber)) {
                    seenNumbers.add(cleanNumber)
                    contactList.add(
                        DeviceContact(
                            name = name.trim(),
                            phoneNumber = cleanNumber
                        )
                    )
                }
            }
        }

        contactList
    }
}
