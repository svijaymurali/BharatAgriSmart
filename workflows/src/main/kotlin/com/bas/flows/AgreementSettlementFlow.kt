package com.bas.flows


import co.paralleluniverse.fibers.Suspendable
import com.bas.states.FarmAgreementState
import com.bas.states.FarmProduceState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.workflows.asset.CashUtils
import net.corda.finance.workflows.getCashBalance
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class AgreementSettlementFlow

  //  class Initiator(
    ( private val farmAgreementID: UniqueIdentifier,
            private val settlementAmount: Amount<Currency>) : FlowLogic<SignedTransaction>() {
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

            // Vault is queried to fetch a list of all Farm Agreements state, then the result is filtered using FarmAgreementId
            // to fetch the desired Farm Agreement state from the vault. This filtered state would be used as input to the
            // transaction.

            val query = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(farmAgreementID))
            val farmAgreementStateAndRefs= serviceHub.vaultService.queryBy<FarmAgreementState>(query).states.single()
            val farmAgreementState = farmAgreementStateAndRefs.state.data

            // QueryCriteria to query the Farm Produce.
            // The linear pointer in previously filtered Farm Agreement state is resolved to fetch the farmProduceState containing
            // the Farm produce's unique id.
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                    null,
                    listOf(farmAgreementStateAndRefs.state.data.farmProduceItem!!.resolve(serviceHub).state.data.linearId.id),
                    null, Vault.StateStatus.UNCONSUMED)

            //Vault Query with the previously created queryCriteria to fetch th farmProduce, then it will be used as an input
            // in the transaction.
            val farmProduceStateAndRef = serviceHub.vaultService.queryBy<FarmProduceState>(queryCriteria).states[0]

            // Here we check the cash balance of buyer node before settling the agreement and throwing the exception
            // if there is no sufficient balance
            val cashBalance = serviceHub.getCashBalance(currency = Currency.getInstance("INR"))
            check(cashBalance.quantity > 0L) {
                throw FlowException("You do not have  ${farmAgreementState.agreedAmount.token} to pay.")
            }
            check(cashBalance >= farmAgreementState.agreedAmount) {
                throw FlowException("You have only $cashBalance but needs ${farmAgreementState.agreedAmount} to settle the Farm Agreement.")
            }


            // The withNewOwner() function from the Ownable state is used get the command and the output state
            // to transfer the of farm produce in the below transaction
            val commandAndState = farmProduceStateAndRef.state.data.withNewOwner(farmAgreementState.buyer)


            // Transaction builder is created with input state as farm produce, output state as command state
            // to transfer the ownership and command itself.
            val buildTransaction = TransactionBuilder(notary)
                    .addInputState(farmProduceStateAndRef)
                    .addOutputState(commandAndState.ownableState)
                    .addCommand(commandAndState.command, listOf(farmAgreementState.seller.owningKey))


            // Cash is debited using the Generate spend method and transaction is updated with the parameters provided
            // We are generating new keypair to sign the cash transaction and the change is returned back to buyer
            val transactionAndKeysPair = CashUtils.generateSpend(
                    serviceHub,buildTransaction,
                    settlementAmount,ourIdentityAndCert,
                    farmAgreementState.seller, emptySet())

            val unsignedTx = transactionAndKeysPair.first


            // Transaction is verified
            unsignedTx.verify(serviceHub)

            // Initial transaction signed with the keys generated abobe and our key

            val keysToSign = transactionAndKeysPair.second.plus(ourIdentity.owningKey)
            val partiallySignedTx = serviceHub.signInitialTransaction(unsignedTx,keysToSign)

            // A flow initiated to collect Farmer / Seller signature as counterparty of the transaction.
            val sellerFlow = initiateFlow(farmAgreementState.seller)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, listOf(sellerFlow)))

            // Transaction is notarised and state is recorded in the ledger of participants
            subFlow(ChangeFarmAgreementStatusSettled(farmAgreementID))
            return subFlow(FinalityFlow(fullySignedTx,(sellerFlow)))

        }
    }


@InitiatedBy(AgreementSettlementFlow::class)
class AgreementSettlementFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
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
