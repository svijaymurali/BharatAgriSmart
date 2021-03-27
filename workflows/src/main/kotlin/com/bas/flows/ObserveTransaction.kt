package com.bas.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ReceiveTransactionFlow
import net.corda.core.node.StatesToRecord


/**
 * This flow is counter to TransactionBroadcastFlow. Here observable states feature is used.
 * StatesToRecord.ALL_VISIBLE records all states from the transaction even if the participant was not part of the
 * transaction flow.
 *
 */
@InitiatedBy(TransactionBroadcastFlow::class)
class ObserveTransaction(private val otherSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        // Receive and record the new campaign state in our vault EVEN THOUGH we are not a participant as we are
        // using 'ALL_VISIBLE'.
        val flow = ReceiveTransactionFlow(
                otherSideSession = otherSession,
                checkSufficientSignatures = true,
                statesToRecord = StatesToRecord.ALL_VISIBLE
        )

        subFlow(flow)

    }

}