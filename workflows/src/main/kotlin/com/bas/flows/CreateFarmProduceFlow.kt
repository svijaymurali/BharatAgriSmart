package com.bas.flows

import co.paralleluniverse.fibers.Suspendable
import com.bas.contracts.FarmProduceContract
import com.bas.states.FarmProduceState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class CreateFarmProduceFlow(private val state: FarmProduceState): FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // Obtain a reference from a notary we wish to use.
        /**
         *  METHOD 1: Taking the first notary on network, WARNING: used only for test, non-prod environments, and single-notary networks!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
         *
         *  * - For production we always want to use second Method  as it guarantees the expected notary is returned.
         */
        val notary = serviceHub.networkMapCache.notaryIdentities.first() // METHOD 1
        // val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

        // Command to create farm produce
        val createCommand = Command(FarmProduceContract.Commands.CreateFarmProduce(), state.participants.map { it.owningKey })
        // listOf(ourIdentity.owningKey))


        // Transaction is built, state received from constructor is added and the command as well to the transaction.
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(state, FarmProduceContract.FarmProduce_ContractID)
        builder.addCommand(createCommand)

        // Verify the transaction
//        builder.verify(serviceHub)

        // Sign the transaction
        val partiallySignedTx = serviceHub.signInitialTransaction(builder)
        // val sessions = (state.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        // val signedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, sessions))


        // Transaction is notarised and state is recorded in the ledger.
        val fullySignedTx = subFlow(FinalityFlow(partiallySignedTx, listOf()))

        // Newly created farm produce is broadcast to all nodes in the network
        subFlow(TransactionBroadcastFlow(fullySignedTx))
        return fullySignedTx

    }
}
/*
// Flow responder to sender flow
// Signs the received transaction as a counterparty to complete the transaction

@InitiatedBy(CreateFarmProduceFlow::class)
class CreateFarmProduceFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an FarmProduce create transaction" using (output is FarmProduceState)
            }
        }
        subFlow(signedTransactionFlow)
    }
}

 */