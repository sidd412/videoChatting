package com.videoChatting.echat.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.videoChatting.echat.domain.repository.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatDatabaseHelper @Inject constructor(
    @ApplicationContext context: Context
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "echat_local.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_MESSAGES = "messages"
        private const val KEY_MESSAGE_ID = "messageId"
        private const val KEY_CHAT_ID = "chatId"
        private const val KEY_SENDER_ID = "senderId"
        private const val KEY_RECEIVER_ID = "receiverId"
        private const val KEY_TEXT = "text"
        private const val KEY_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_MESSAGES + "("
                + KEY_MESSAGE_ID + " TEXT PRIMARY KEY,"
                + KEY_CHAT_ID + " TEXT,"
                + KEY_SENDER_ID + " TEXT,"
                + KEY_RECEIVER_ID + " TEXT,"
                + KEY_TEXT + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER" + ")")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    // Insert single message
    fun insertMessage(message: Message) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_MESSAGE_ID, message.messageId)
            put(KEY_CHAT_ID, message.chatId)
            put(KEY_SENDER_ID, message.senderId)
            put(KEY_RECEIVER_ID, message.receiverId)
            put(KEY_TEXT, message.text)
            put(KEY_TIMESTAMP, message.timestamp)
        }
        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Insert list of messages (bulk insert)
    fun insertMessages(messages: List<Message>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            for (message in messages) {
                val values = ContentValues().apply {
                    put(KEY_MESSAGE_ID, message.messageId)
                    put(KEY_CHAT_ID, message.chatId)
                    put(KEY_SENDER_ID, message.senderId)
                    put(KEY_RECEIVER_ID, message.receiverId)
                    put(KEY_TEXT, message.text)
                    put(KEY_TIMESTAMP, message.timestamp)
                }
                db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // Fetch messages for a specific chat ID, sorted by timestamp ascending
    fun getMessages(chatId: String): List<Message> {
        val messagesList = mutableListOf<Message>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_MESSAGES WHERE $KEY_CHAT_ID = ? ORDER BY $KEY_TIMESTAMP ASC"
        val cursor = db.rawQuery(selectQuery, arrayOf(chatId))

        if (cursor.moveToFirst()) {
            do {
                val message = Message(
                    messageId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MESSAGE_ID)),
                    chatId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHAT_ID)),
                    senderId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SENDER_ID)),
                    receiverId = cursor.getColumnIndex(KEY_RECEIVER_ID).let { if (it != -1) cursor.getString(it) else "" },
                    text = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TEXT)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
                )
                messagesList.add(message)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return messagesList
    }
}
