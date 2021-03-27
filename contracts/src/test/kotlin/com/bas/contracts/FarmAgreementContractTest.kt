package com.bas.contracts

import net.corda.core.identity.CordaX500Name
import com.bas.states.FarmAgreementState
import com.bas.states.FarmAgreementStatus
import net.corda.core.contracts.*
import net.corda.finance.contracts.Commodity
import net.corda.finance.contracts.Tenor
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.math.BigDecimal
import java.util.*
import net.corda.testing.contracts.DummyState
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class FarmAgreementContractTest {
    private val farmer1 = TestIdentity(CordaX500Name("Farmer1", "", "IN"))
    private val farmer2 = TestIdentity(CordaX500Name("Farmer2", "", "DE"))
    private val organicMarket1 = TestIdentity(CordaX500Name("OrganicMarket1", "", "US"))
    private val organicMarket2 = TestIdentity(CordaX500Name("OrganicMarket2", "", "GB"))

    // private val ledgerServices = MockServices(TestIdentity(CordaX500Name("TestId", "", "GB")))
    private val ledgerServices = MockServices(listOf("com.bas.contracts"))

    /**
     *New farm agreement transactions should not have any input state references. Hence we ensure that
     * no input states are included in a transaction while proposing a new farmagreement
     */
    @Test
    fun createFarmAgreementTransactionMustHaveNoInputs() {
        val farmAgreement1  = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)

        ledgerServices.ledger {
            transaction {
                input(FarmAgreementContract.FarmAgreementContractID, DummyState())
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                output(FarmAgreementContract.FarmAgreementContractID, farmAgreement1)
                this `fails with` "No inputs should be consumed when creating an new Farmagreement."
            }
            transaction {
                output(FarmAgreementContract.FarmAgreementContractID, farmAgreement1)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                this.verifies() // As there are no input states.
            }
        }
    }

    /**
     *Newly proposed farm agreement transactions should  have only one output state. Hence we ensure that
     * only one output states are included in a transaction while proposing farm agreement
     */
    @Test
    fun createFarmAgreementTransactionMustHaveOneOutput() {

        val farmAgreement1  = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)
        val farmAgreement2  = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((5500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket2.party,farmer2.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)

        ledgerServices.ledger {
            transaction {
                command(listOf(farmer1.publicKey,farmer2.publicKey,organicMarket1.publicKey,organicMarket2.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                output(FarmAgreementContract.FarmAgreementContractID, farmAgreement1) // here we have two outputs, hence it fails.
                output(FarmAgreementContract.FarmAgreementContractID, farmAgreement2)
                this `fails with` "Only one Farm Agreement should be created when issuing an new agreement."
            }
            transaction {
                command(listOf(farmer2.publicKey,organicMarket2.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                output(FarmAgreementContract.FarmAgreementContractID, farmAgreement2)   // here only one output, hence passes.

                this.verifies()
            }
        }
    }

    /**
     *Newly proposed farm agreement transactions should  have only positive agreed amount.
     */
    @Test
    fun farmAgreementMustHavePositiveAgreedAmount() {
        val  passingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)
        val  failingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((0.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)

        ledgerServices.ledger {

            // Should fail since agreed amount is 0
            transaction {
                output(FarmAgreementContract.FarmAgreementContractID, failingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                this `fails with` "A newly issued Farm agreement must have a positive amount."
            }
            //this transaction will pass since agreed amount is positive and greater than 0
            transaction {
                output(FarmAgreementContract.FarmAgreementContractID, passingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                verifies()
            }
        }
    }

    /**
     *Newly proposed farm agreement transactions should have different buyer and seller(buyer != seller).
     */
    @Test
    fun farmAgreementMustHaveDifferentBuyerSeller() {
        val  passingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)
        val  failingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,organicMarket1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)


        ledgerServices.ledger {

            // Should fail since both buyer and seller are same entitys
            transaction {
                output(FarmAgreementContract.FarmAgreementContractID, failingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                this `fails with` "The seller and buyer cannot be the same identity."
            }
            //this transaction will pass
            transaction {
                output(FarmAgreementContract.FarmAgreementContractID, passingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                verifies()
            }
        }
    }

    /**
     *Newly proposed farm agreement transactions should have agreement status = IN_PROPOSAL
     */
    @Test
    fun newFarmAgreementMustHaveINPROPOSALState() {
        val  passingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)
        val  failingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_CONTRACT)


        ledgerServices.ledger {

            // Should fail since newly proposed farm agreement is not in IN_PROPOSAL state
            transaction {
                output(FarmAgreementContract.FarmAgreementContractID, failingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                this `fails with` "A newly proposed Farm Agreement should be in IN_PROPOSAL state"
            }
            //this transaction will pass
            transaction {
                output(FarmAgreementContract.FarmAgreementContractID, passingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                verifies()
            }
        }
    }


    /**
     *Newly proposed farm agreement transactions should have legal prose attachement hash
     */
    @Test
    fun newFarmAgreementMustHaveLegalProseAttachmentHash() {
        val  passingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)
        val  failingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","",FarmAgreementStatus.IN_PROPOSAL)


        ledgerServices.ledger {

            // Should fail since legal prose attachment hash is empty.
            transaction {
                output(FarmAgreementContract.FarmAgreementContractID, failingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                this `fails with` "Legal Prose Attachment Hash must be stored in state"
            }
            //this transaction will pass
            transaction {
                output(FarmAgreementContract.FarmAgreementContractID, passingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.ProposeAgreement())
                verifies()
            }
        }
    }


    /**
     *Accepted farm agreement status should change only from IN_PROPOSAL(input state) to IN_CONTRACT (output state)
     */
    @Test
    fun acceptedFarmAgreementMustChangeToINCONTRACTState() {
        val  inputState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)
        val  outputState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_CONTRACT)
        val  failingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.CANCELLED)

        ledgerServices.ledger {

            // Should fail because input farm agreement is not IN_PROPOSAL
            transaction {
                input(FarmAgreementContract.FarmAgreementContractID, failingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.SetFarmAgreementStatusInContract())
                output(FarmAgreementContract.FarmAgreementContractID, outputState)
                 this `fails with` "Input Agreement state should be IN_PROPOSAL"
            }
            //this transaction will pass
            transaction {
                input(FarmAgreementContract.FarmAgreementContractID, inputState)
                output(FarmAgreementContract.FarmAgreementContractID, outputState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.SetFarmAgreementStatusInContract())
                verifies()
            }
        }
    }


    /**
     *A farm agreement which is to be settled should have input status IN_CONTRACT(input state) and it should change to SETTLED (output state)
     */
    @Test
    fun settlementFarmAgreementMustChangeToSETTLEDState() {
        val  inputState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_CONTRACT)
        val  outputState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.SETTLED)
        val  failingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_PROPOSAL)

        ledgerServices.ledger {

            // Should fail because input farm agreement is not IN_CONTRACT
            transaction {
                input(FarmAgreementContract.FarmAgreementContractID, failingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.SetFarmAgreementStatusSettled())
                output(FarmAgreementContract.FarmAgreementContractID, outputState)
                this `fails with` "Input Agreement state should be IN_CONTRACT"
            }
            //this transaction will pass
            transaction {
                input(FarmAgreementContract.FarmAgreementContractID, inputState)
                output(FarmAgreementContract.FarmAgreementContractID, outputState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.SetFarmAgreementStatusSettled())
                verifies()
            }
        }
    }

    /**
     *A farm agreement which is to be cancelled should have input status IN_CONTRACT(input state) and it should change to CANCELLED (output state)
     */
    @Test
    fun toBeCancelledFarmAgreementMustChangeToCANCELLEDState() {
        val  inputState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.IN_CONTRACT)
        val  outputState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.CANCELLED)
        val  failingState = FarmAgreementState(LinearPointer(UniqueIdentifier(), LinearState::class.java),UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")),Amount((4500.toLong()), BigDecimal(100), Currency.getInstance("INR")),LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),organicMarket1.party,farmer1.party, Tenor("3Y"),"3% of Market","TestHash",FarmAgreementStatus.SETTLED)

        ledgerServices.ledger {

            // Should fail because input farm agreement is not IN_CONTRACT
            transaction {
                input(FarmAgreementContract.FarmAgreementContractID, failingState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.SetFarmAgreementStatusCancelled())
                output(FarmAgreementContract.FarmAgreementContractID, outputState)
                this `fails with` "Input Farm Agreement state should be IN_CONTRACT"
            }
            //this transaction will pass
            transaction {
                input(FarmAgreementContract.FarmAgreementContractID, inputState)
                output(FarmAgreementContract.FarmAgreementContractID, outputState)
                command(listOf(farmer1.publicKey,organicMarket1.publicKey), FarmAgreementContract.Commands.SetFarmAgreementStatusCancelled())
                verifies()
            }
        }
    }
}