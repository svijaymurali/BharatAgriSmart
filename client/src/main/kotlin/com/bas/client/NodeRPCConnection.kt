package com.bas.client

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private const val USER_NAME = "config.rpc.username"
private const val PASSWORD = "config.rpc.password"
private const val HOST = "config.rpc.host"
private const val RPC_PORT = "config.rpc.port"

/**
 * Creates an RPC connection to node.
 *
 * @param host species to the node about host we are connecting to.
 * @param rpcPort  RPC port of the node we are connecting to.
 * @param username username for logging into the RPC client.
 * @param password password for logging into the RPC client.
 * @property proxy RPC proxy.
 */
@Component
open class NodeRPCConnection(
        @Value("\${$HOST}") private val host: String,
        @Value("\${$USER_NAME}") private val username: String,
        @Value("\${$PASSWORD}") private val password: String,
        @Value("\${$RPC_PORT}") private val rpcPort: Int): AutoCloseable {

    lateinit var rpcConnection: CordaRPCConnection
        private set
    lateinit var proxy: CordaRPCOps
        private set

    @PostConstruct
    fun initialiseNodeRPCConnection() {
        val rpcAddress = NetworkHostAndPort(host, rpcPort)
        val rpcClient = CordaRPCClient(rpcAddress)
        val rpcConnection = rpcClient.start(username, password)
        proxy = rpcConnection.proxy
    }

    @PreDestroy
    override fun close() {
        rpcConnection.notifyServerAndClose()
    }
}