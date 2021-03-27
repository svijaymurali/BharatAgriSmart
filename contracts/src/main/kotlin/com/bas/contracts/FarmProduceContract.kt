package com.bas.contracts

import com.bas.states.FarmProduceState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand


// ************
// * Contract *
// ************

class FarmProduceContract : Contract {

    companion object {

        // Used to identify our contract when building a transaction.
        const val FarmProduce_ContractID = "com.bas.contracts.FarmProduceContract"
    }


    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class CreateFarmProduce: TypeOnlyCommandData(), Commands
        class TransferFarmProduce : TypeOnlyCommandData(), Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.

    override fun verify(tx: LedgerTransaction) {

        // Verification logic goes here.

        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.CreateFarmProduce -> requireThat {
                "No inputs should be consumed when creating an new FarmProduce Asset." using (tx.inputs.isEmpty())
                "Only one output state should be created when creating an new FarmProduce Asset." using (tx.outputs.size == 1)
                val farmProduceOutput = tx.outputStates.single() as FarmProduceState
                "A newly created FarmProduce must have a positive weight." using (farmProduceOutput.weightInKG > 0 )
                "A newly created FarmProduce must have a positive minimum guaranteed amount." using (farmProduceOutput.minGuaranteedPrice.quantity > 0 )
                "Seller has to sign the create farm produce transaction." using
                        (command.signers.toSet() == farmProduceOutput.participants.map { it.owningKey }.toSet())
            }


            // Another way of transferring ownership. Since in-built function
            // fun withNewOwner(newOwner: AbstractParty): CommandAndState is used in state, below function is commented.

            /*   is Commands.TransferFarmProduce -> requireThat {

                   val farmProduceInputs = tx.inputsOfType<FarmProduceState>()
                   val farmProduceOutputs = tx.outputsOfType<FarmProduceState>()

                   "A FarmProduce transfer transaction should only consume one input state." using (farmProduceInputs.size == 1)
                   "A FarmProduce transfer transaction should only create one output state." using (farmProduceOutputs.size == 1)
                   val input = farmProduceInputs.single()
                   val output = farmProduceOutputs.single()
                   "Only the owner of the Farm Produce may change." using (input == output.withNewOwner(input.owner))
                   "The owner of the Farm Produce must change in a transfer." using (input.owner != output.owner)
               }  */
        }


    }

}