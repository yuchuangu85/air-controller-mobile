package com.youngfeng.android.assistant.socket

import android.util.Log
import timber.log.Timber
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * 用于快速创建Socket Server，处理通用逻辑。
 *
 * @author Scott Smith 2021/12/29 16:13
 */
class SimpleSocketServer(private val port: Int) {
    private val mExecutorService = Executors.newCachedThreadPool()
    private var mServerSocket: ServerSocket? = null
    private var isStarted = false
    private var onStartComplete: ((server: ServerSocket) -> Unit)? = null
    private var onStartFail: ((error: String) -> Unit)? = null
    private var onMessage: ((client: Socket, data: ByteArray) -> Unit)? = null
    private var onStopComplete: (() -> Unit)? = null
    private val mClients = mutableListOf<Socket>()
    var onSecondaryClientEnter: ((client: Socket) -> Unit)? = null
    var onClientConnect: ((client: Socket) -> Unit)? = null
    var onClientDisconnect: ((client: Socket) -> Unit)? = null

    companion object {
        private val TAG = SimpleSocketServer::class.simpleName
    }

    fun start() {
        mExecutorService.submit {
            try {
                mServerSocket = ServerSocket(port)
                onStartComplete?.invoke(mServerSocket!!)
                isStarted = true

                while (isStarted) {
                    val client = mServerSocket!!.accept()
                    if (mClients.isNotEmpty()) {
                        onSecondaryClientEnter?.invoke(client)
                        client.close()
                    } else {
                        Log.d(TAG, "Client added: $client")
                        onClientConnect?.invoke(client)
                        mClients.add(client)
                        handleMessage(client)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Timber.e("IOException: ${e.message}")
                onStartFail?.invoke(e.message ?: "Unknown error")
            }
        }
    }

    private fun handleMessage(client: Socket) {
        mExecutorService.submit {
            val inputStream = client.getInputStream()
            do {
                val data = mutableListOf<Byte>()

                val buffer = ByteArray(1024)

                val bytesRead = inputStream.read(buffer)
                if (-1 != bytesRead) {
                    data.addAll(buffer.slice(0 until bytesRead))

                    Log.d(TAG, "handleMessage, port: ${client.localPort}, data length: ${data.size}, value: ${String(data.toByteArray())}, $onMessage")
                    onMessage?.invoke(client, data.toByteArray())
                } else {
                    mClients.remove(client)
                    onClientDisconnect?.invoke(client)
                    Log.d(TAG, "Remove this client [$client], bytesRead = -1")
                }
            } while (bytesRead != -1)
        }
    }

    fun stop() {
        try {
            mServerSocket?.close()
            if (mClients.isNotEmpty()) {
                onClientDisconnect?.invoke(mClients.single())
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Timber.e("Stop server socket cause error: ${e.message}")
        }
        mExecutorService.shutdown()
        isStarted = false
        onStopComplete?.invoke()
    }

    fun sendToClient(client: Socket, data: ByteArray) {
        mExecutorService.submit {
            try {
                val outputStream = client.getOutputStream()
                outputStream.write(data)
                outputStream.flush()
            } catch (e: IOException) {
                e.printStackTrace()
                mClients.remove(client)
                onClientDisconnect?.invoke(client)
                Timber.e("send to client [$client] fail, reason: ${e.message}")
            }
        }
    }

    fun sendToAllClient(data: ByteArray) {
        if (mClients.isNotEmpty()) {
            mClients.forEach {
                sendToClient(it, data)
            }
        }
    }

    fun onStartComplete(callback: (server: ServerSocket) -> Unit) {
        this.onStartComplete = callback
    }

    fun onStartFail(callback: (error: String) -> Unit) {
        this.onStartFail = callback
    }

    fun onMessage(callback: (client: Socket, data: ByteArray) -> Unit) {
        this.onMessage = callback
    }

    fun onStopComplete(callback: () -> Unit) {
        this.onStopComplete = callback
    }

    fun disconnect() {
        if (mClients.isNotEmpty()) {
            try {
                mClients.single().close()
                mClients.clear()
            } catch (e: Exception) {
                Log.e(TAG, "${e.message}")
            }
        }
    }

    fun isStarted(): Boolean {
        return isStarted
    }

    fun getHostname() = mServerSocket?.inetAddress?.hostName
}
