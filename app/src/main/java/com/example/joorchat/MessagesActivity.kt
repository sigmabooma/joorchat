package com.example.joorchat

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.TextMessage
import com.cometchat.pro.models.BaseMessage
import com.cometchat.pro.core.MessagesRequest


class MessagesActivity : AppCompatActivity() {

    private lateinit var enterMessage: EditText
    private lateinit var send: Button
    private lateinit var messagesList: RecyclerView
    private lateinit var messagesAdapter: MessagesAdapter
    private val listenerID = "MESSAGES_LISTENER"
    private val roomID = "groupie"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        enterMessage = findViewById(R.id.enter_message)
        send = findViewById(R.id.send_message)

        messagesList = findViewById(R.id.messages)
        val layoutMgr = LinearLayoutManager(this)
        layoutMgr.stackFromEnd = true
        messagesList.layoutManager = layoutMgr

        messagesAdapter = MessagesAdapter(CometChat.getLoggedInUser().uid, mutableListOf())
        messagesList.adapter = messagesAdapter

        send.setOnClickListener {
            sendMessage()
        }

        joinGroup()
    }

    override fun onResume() {
        super.onResume()
        CometChat.addMessageListener(listenerID, object : CometChat.MessageListener() {
            override fun onTextMessageReceived(message: TextMessage) {
                messagesAdapter.appendMessage(message)
                scrollToBottom()
            }
        })
    }

    override fun onPause() {
        super.onPause()
        CometChat.removeMessageListener(listenerID)
    }


    private fun joinGroup() {
        CometChat.joinGroup(
            roomID,
            CometChatConstants.GROUP_TYPE_PUBLIC, "", object : CometChat.CallbackListener<String>() {
                override fun onSuccess(successMessage: String) {
                    fetchMessages()
                    //Log.d("CometChat", "Group join successful")
                }

                override fun onError(e: CometChatException) {
                    e.code?.let {
                        // For now, we'll just keep on attempting to join the group
                        // because persistence is out of the scope for this tutorial
                        if (it.contentEquals("ERR_ALREADY_JOINED")) {
                            fetchMessages()
                       //     Log.d("CometChat", "Already joined the group")
                        }
                    }
                }
            })
    }

    private fun sendMessage() {
        val textMessage = TextMessage(roomID, enterMessage.text.toString(),
            CometChatConstants.MESSAGE_TYPE_TEXT,
            CometChatConstants.RECEIVER_TYPE_GROUP
        )

        CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
            override fun onSuccess(message: TextMessage) {
                enterMessage.setText("")
                messagesAdapter.appendMessage(message)
                scrollToBottom()
            }

            override fun onError(e: CometChatException) {
                Log.d("CometChat", "Message send failed: ${e.message}")
            }
        })
    }


    private fun scrollToBottom() {
        messagesList.scrollToPosition(messagesAdapter.itemCount - 1)
    }

    private fun fetchMessages() {
        val messagesRequest = MessagesRequest.MessagesRequestBuilder()
            .setGUID(roomID)
            .setLimit(30)
            .build()

        messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage>>() {
            override fun onSuccess(messages: List<BaseMessage>) {
                messagesAdapter.updateMessages(messages.filter { it is TextMessage })
                scrollToBottom()
            }

            override fun onError(e: CometChatException) {
                Log.d("CometChat", "Fetch messages failed: ${e.message}")
            }
        })
    }

}
