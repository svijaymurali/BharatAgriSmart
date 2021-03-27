package com.bas.contracts

import com.bas.states.FarmAgreementState
import com.bas.states.FarmAgreementStatus
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

// ************
// * Contract *
// ************

@LegalProseReference(uri = "legal.html")
class FarmAgreementContract : Contract {
    companion object {

        // Used to identify our contract when building a transaction.
        const val FarmAgreementContractID = "com.bas.contracts.FarmAgreementContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class ProposeAgreement : Commands
        class SetFarmAgreementStatusInContract : Commands
        class SettleFarmAgreement : Commands
        class SetFarmAgreementStatusSettled : Commands
        class SetFarmAgreementStatusCancelled : Commands

    }
    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        
        // Verification logic goes here.
        
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.ProposeAgreement -> verifyProposeAgreement(tx, setOfSigners)
            is Commands.SetFarmAgreementStatusInContract -> verifySetFarmAgreementStatusInContract(tx, setOfSigners)
            is Commands.SettleFarmAgreement -> verifySettleFarmAgreement(tx, setOfSigners)
            is Commands.SetFarmAgreementStatusSettled -> verifySetFarmAgreementStatusSettled(tx, setOfSigners)
            is Commands.SetFarmAgreementStatusCancelled -> verifySetFarmAgreementStatusCancelled(tx, setOfSigners)

            else -> throw IllegalArgumentException("Unrecognised command")
        }
    }

    private fun keysFromParticipants(farmAgreement: FarmAgreementState): Set<PublicKey> {
        return farmAgreement.participants.map {
            it.owningKey
        }.toSet()
    }

    // Function to verify propose farm agreement
    private fun verifyProposeAgreement(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs should be consumed when creating an new Farmagreement." using (tx.inputs.isEmpty())
        "Only one Farm Agreement should be created when issuing an new agreement." using (tx.outputStates.size == 1)
        val farmAgreement = tx.outputsOfType<FarmAgreementState>().single()

        "A newly issued Farm agreement must have a positive amount." using (farmAgreement.agreedAmount.quantity > 0)

        "The seller and buyer cannot be the same identity." using (farmAgreement.seller != farmAgreement.buyer)

        "A newly proposed Farm Agreement should be in IN_PROPOSAL state" using (farmAgreement.status == FarmAgreementStatus.IN_PROPOSAL)

        "Legal Prose Attachment Hash must be stored in state" using (farmAgreement.legalProseAttachmentHash.isNotEmpty())

        "Both seller and buyer together only may sign FarmAgreement issue transaction." using
                (signers == keysFromParticipants(farmAgreement))

            }


    // Function to verify the change of farm agreement status to IN_CONTRACT from proposal state
    private fun verifySetFarmAgreementStatusInContract(tx: LedgerTransaction, signers: Set<PublicKey>) {
        "One input state should be consumed when changing farm agreement status" using (tx.inputStates.size == 1)
        "One output state should be created when changing farm agreement status" using (tx.outputStates.size == 1)
        val inputfarmAgreements = tx.inputsOfType<FarmAgreementState>()
        val inputfarmAgreementstate = inputfarmAgreements.single()
        val outputfarmAgreements = tx.outputsOfType<FarmAgreementState>()
        val outputfarmAgreementstate = outputfarmAgreements.single()

        "Input Agreement state should be IN_PROPOSAL" using (inputfarmAgreementstate.status == FarmAgreementStatus.IN_PROPOSAL)
        "Output Agreement state is changed to SETTLED" using (outputfarmAgreementstate.status == FarmAgreementStatus.IN_CONTRACT)

        "Seller and Buyer must Sign" using (signers == keysFromParticipants(inputfarmAgreementstate))
    }

    // Function to verify farm agreement settlement
    private fun verifySettleFarmAgreement(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {

        val farmAgreements = tx.inputsOfType<FarmAgreementState>()
        val inputfarmAgreements = farmAgreements.single()

        "Input Agreement state should be IN_CONTRACT" using (inputfarmAgreements.status == FarmAgreementStatus.IN_CONTRACT)
        "Seller and Buyer must Sign" using (signers == keysFromParticipants(inputfarmAgreements))
            }

    // Function to verify the change of farm agreement status to SETTLED from IN_CONTRACT
    private fun verifySetFarmAgreementStatusSettled(tx: LedgerTransaction, signers: Set<PublicKey>) {
        "One input state should be consumed when changing farm agreement status" using (tx.inputStates.size == 1)
        "One output state should be created when changing farm agreement status" using (tx.outputStates.size == 1)

        val inputfarmAgreements = tx.inputsOfType<FarmAgreementState>()
        val inputfarmAgreementstate = inputfarmAgreements.single()
        val outputfarmAgreements = tx.outputsOfType<FarmAgreementState>()
        val outputfarmAgreementstate = outputfarmAgreements.single()

        "Input Agreement state should be IN_CONTRACT" using (inputfarmAgreementstate.status == FarmAgreementStatus.IN_CONTRACT)
        "Output Agreement state is SETTLED" using (outputfarmAgreementstate.status == FarmAgreementStatus.SETTLED)

        "Seller and Buyer must Sign" using (signers == keysFromParticipants(inputfarmAgreementstate))
    }


    // Function to verify the change of farm agreement status to CANCELLED from IN_CONTRACT
    private fun verifySetFarmAgreementStatusCancelled(tx: LedgerTransaction, signers: Set<PublicKey>) {
        "One input state should be consumed when changing farm agreement status" using (tx.inputStates.size == 1)
        "One output state should be created when changing farm agreement status" using (tx.outputStates.size == 1)
        val inputfarmAgreements = tx.inputsOfType<FarmAgreementState>()
        val inputfarmAgreementstate = inputfarmAgreements.single()
        val outputfarmAgreements = tx.outputsOfType<FarmAgreementState>()
        val outputfarmAgreementstate = outputfarmAgreements.single()

        "Input Farm Agreement state should be IN_CONTRACT" using (inputfarmAgreementstate.status == FarmAgreementStatus.IN_CONTRACT)
        "Output Farm Agreement state is CANCELLED" using (outputfarmAgreementstate.status == FarmAgreementStatus.CANCELLED)

        "Seller and Buyer must Sign" using
                (signers == keysFromParticipants(inputfarmAgreementstate))
    }




}