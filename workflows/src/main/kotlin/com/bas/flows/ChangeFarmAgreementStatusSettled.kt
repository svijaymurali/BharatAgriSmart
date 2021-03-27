package com.bas.flows

import co.paralleluniverse.fibers.Suspendable
import com.bas.contracts.FarmAgreementContract
import com.bas.states.FarmAgreementStatus
import com.bas.states.FarmAgreementState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

// *********
// * Flows *
// *********

@InitiatingFlow
@StartableByRPC
class ChangeFarmAgreementStatusSettled

 //   open class Initiator
    (private val farmAgreementID: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {

            // Obtain a reference from a notary we wish to use.
            /**
             *  METHOD 1: Taking the first notary on network, WARNING: used only for test, non-prod environments, and single-notary networks!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production we always want to use second Method  as it guarantees the expected notary is returned.
             */
            val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()// METHOD 1
            // val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

            // Vault is queried to get a list of all FarmAgreement state, the result is filtered based on farmAgreementID
            // in order to get a particular state data from the vault.
            // This filtered state would act as an input Farm Agreement State for the upcoming transaction

            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(farmAgreementID))
            val farmAgreementInputStateAndRef = serviceHub.vaultService.queryBy<FarmAgreementState>(queryCriteria).states.single()
            val inputFarmAgreementState = farmAgreementInputStateAndRef.state.data

            val setStatusCommand = Command(FarmAgreementContract.Commands.SetFarmAgreementStatusSettled(), inputFarmAgreementState.participants.map { it.owningKey })
            val outputState = inputFarmAgreementState.copy(status = FarmAgreementStatus.SETTLED)
            val outputStateAndContract = StateAndContract(outputState, FarmAgreementContract.FarmAgreementContractID)

            val unsignedTx = TransactionBuilder(notary = notary).withItems(
                    outputStateAndContract,
                    farmAgreementInputStateAndRef,
                    setStatusCommand
            )

            // Transaction is verified
            unsignedTx.verify(serviceHub)

            // Transaction is signed with our key.
            val partiallySignedTx = serviceHub.signInitialTransaction(unsignedTx)

            // A flow initiated to collect Farmer / Seller signature as counterparty of the transaction.
            val sellerFlow = initiateFlow(inputFarmAgreementState.seller)
            val signedTx = subFlow(CollectSignaturesFlow(
                    partiallySignedTx,
                    listOf(sellerFlow)

            ))

            // Transaction is notarised and state is recorded in the ledger of participants
            return subFlow(FinalityFlow(signedTx,sellerFlow))

        }
    }

// Flow responder to sender flow
// Signs the received transaction as a counterparty to complete the transaction

@InitiatedBy(ChangeFarmAgreementStatusSettled::class)
class ChangeFarmAgreementStatusSettledFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
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
