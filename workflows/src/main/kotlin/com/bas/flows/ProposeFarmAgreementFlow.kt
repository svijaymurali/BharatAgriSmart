package com.bas.flows

import co.paralleluniverse.fibers.Suspendable
import com.bas.contracts.FarmAgreementContract
import com.bas.states.FarmAgreementState
import com.bas.states.FarmAgreementStatus
import com.bas.states.FarmProduceState
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.seconds
import net.corda.finance.contracts.Commodity
import net.corda.finance.contracts.Tenor
import java.io.File
import java.util.*


// *********
// * Flows *
// *********

class ProposeFarmAgreementFlow {
    @InitiatingFlow
    @StartableByRPC
    open class Initiator
    (
            private val farmProduceReferenceID: UniqueIdentifier,
            private val agreedAmount: Amount<Currency>,
            private val durationOfAgreement: Tenor?,
            private val bonusCondition: String,
            private val filePath: String ) : FlowLogic<SignedTransaction>() {

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
            val notary = serviceHub.networkMapCache.notaryIdentities.single()// METHOD 1
            // val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

            // Adding legal prose attachment via private method and creating secure hash for it.

                val legalProseAttachmenthash = SecureHash.parse(addAttachment(filePath,
                        serviceHub,
                        ourIdentity,
                        "legalprosezip"))

            // Using the farmProduce reference we filter the state and data to use it in the agreement
            val query = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(farmProduceReferenceID))
            val farmProduceStateAndRefs= serviceHub.vaultService.queryBy<FarmProduceState>(query).states.single()
            val farmProduceState = farmProduceStateAndRefs.state.data

            // FarmAgreement state is built with Linear pointer pointing to the farm produce.
            val farmAgreement = FarmAgreementState(LinearPointer(UniqueIdentifier(null, farmProduceReferenceID.id), LinearState::class.java),
                    UniqueIdentifier(), farmProduceState.farmProduce, farmProduceState.weightInKG, farmProduceState.minGuaranteedPrice, agreedAmount,farmProduceState.expectedDelivery,farmProduceState.landAddress,farmProduceState.isCropInsured, ourIdentity, farmProduceState.owner as Party, durationOfAgreement,bonusCondition,legalProseAttachmenthash.toString(),FarmAgreementStatus.IN_PROPOSAL)

            val ourSigningKey = farmAgreement.buyer.owningKey

            // Transaction is built, state received from constructor is added and the command as well to the transaction.

            // Generating an unsigned transaction
            val unsignedTx = TransactionBuilder(notary)
                    .addOutputState(farmAgreement,FarmAgreementContract.FarmAgreementContractID)
                    .addCommand(FarmAgreementContract.Commands.ProposeAgreement(),farmAgreement.participants.map { it.owningKey })
                    .setTimeWindow(serviceHub.clock.instant(), 30.seconds)


            // Transaction is signed with our key.
            val partiallySignedTx = serviceHub.signInitialTransaction(unsignedTx,ourSigningKey)


            // A flow initiated to collect Farmer / Seller signature as counterparty of the transaction.

            val sellerFlow = initiateFlow(farmAgreement.seller)
            val fullySignedTx = subFlow(CollectSignaturesFlow(
                    partiallySignedTx,
                    listOf(sellerFlow))
            )

            // Transaction is notarised and state is recorded in the ledger of participants
            return subFlow(FinalityFlow(fullySignedTx, (sellerFlow)))

        }


    }

}

// Private helper method
private fun addAttachment(
        path: String,
        service: ServiceHub,
        whoAmI: Party,
        filename: String
): String {
    val legalProseHash = service.attachments.importAttachment(
            File(path).inputStream(),
            whoAmI.toString(),
            filename)

    return legalProseHash.toString()
}

// Flow responder to sender flow
// Signs the received transaction as a counterparty to complete the transaction

@InitiatedBy(ProposeFarmAgreementFlow.Initiator::class)
class ProposeFarmAgreementFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call():SignedTransaction {
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
            }
        })
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}


