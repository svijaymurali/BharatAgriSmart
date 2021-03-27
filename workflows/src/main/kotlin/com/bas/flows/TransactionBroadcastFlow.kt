package com.bas.flows


import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SendTransactionFlow
import net.corda.core.transactions.SignedTransaction

/**
 * In Corda by default transactions are not broadcasted to all participants but only to involved parties!
 * We filter notaries and our identity then the [SignedTransaction] is broadcasted to all the nodes in network.
 */
@InitiatingFlow
class TransactionBroadcastFlow(val stx: SignedTransaction) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        // All the participants are collected from the network map cache.
        val allParticipates = serviceHub.networkMapCache.allNodes.flatMap { it.legalIdentities }

        // Here we filter only targetParticipants from the above list removing notaries and our identity
        val targetParticipants = allParticipates.filter { serviceHub.networkMapCache.isNotary(it).not() } - ourIdentity

        // A flow session is created for targetParticipants
        val sessions = targetParticipants.map { initiateFlow(it) }

        // Signed transaction is broadcast to participants.
        sessions.forEach { subFlow(SendTransactionFlow(it, stx)) }
    }

}