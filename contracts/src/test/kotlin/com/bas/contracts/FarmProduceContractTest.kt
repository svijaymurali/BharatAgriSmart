package com.bas.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import com.bas.states.FarmProduceState
import net.corda.core.contracts.Amount
import net.corda.finance.contracts.Commodity
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import net.corda.testing.contracts.DummyState


class FarmProduceContractTest {
    private val farmer1 = TestIdentity(CordaX500Name("Farmer1", "", "IN"))
    private val farmer2 = TestIdentity(CordaX500Name("Farmer2", "", "DE"))
   // private val ledgerServices = MockServices(TestIdentity(CordaX500Name("TestId", "", "GB")))
    private val ledgerServices = MockServices(listOf("com.bas.contracts"))



    /**
     *New farm produce transactions should not have any input state references. Hence we ensure that
     * no input states are included in a transaction while creating a farm produce
     */
    @Test
    fun createFarmProduceTransactionMustHaveNoInputs() {
        val farmProduce1  = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1.party)

        ledgerServices.ledger {
            transaction {
                input(FarmProduceContract.FarmProduce_ContractID, DummyState())
                command(listOf(farmer1.publicKey), FarmProduceContract.Commands.CreateFarmProduce())
                output(FarmProduceContract.FarmProduce_ContractID, farmProduce1)
                this `fails with` "No inputs should be consumed when creating an new FarmProduce Asset."
            }
            transaction {
               output(FarmProduceContract.FarmProduce_ContractID, farmProduce1)
                command(listOf(farmer1.publicKey), FarmProduceContract.Commands.CreateFarmProduce())
                this.verifies() // As there are no input states.
            }
        }
    }

    /**
     *New farm produce transactions should  have only one output state. Hence we ensure that
     * only one output states are included in a transaction while creating a farm produce
     */
    @Test
    fun createFarmProduceTransactionMustHaveOneOutput() {

        val farmProduce1  = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1.party)
        val farmProduce2  = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer2.party)

        ledgerServices.ledger {
            transaction {
                command(listOf(farmer1.publicKey,farmer2.publicKey), FarmProduceContract.Commands.CreateFarmProduce())
                output(FarmProduceContract.FarmProduce_ContractID, farmProduce1) // here we have two outputs, hence it fails.
                output(FarmProduceContract.FarmProduce_ContractID, farmProduce2)
                this `fails with` "Only one output state should be created when creating an new FarmProduce Asset."
            }
            transaction {
                command(listOf(farmer1.publicKey), FarmProduceContract.Commands.CreateFarmProduce())
                output(FarmProduceContract.FarmProduce_ContractID, farmProduce1)   // here only one output, hence passes.

                this.verifies()
            }
        }
    }

    /**
     *New farm produce transactions should  have only positive weight in kilograms.
     */
    @Test
    fun farmProduceMustHavePositiveWeight() {
        val  passingState = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1.party)
        val  failingState = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),-3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1.party)


        ledgerServices.ledger {

            // Should fail since farm produce has negative weight
            transaction {
                output(FarmProduceContract.FarmProduce_ContractID, failingState)
                command(listOf(farmer1.publicKey), FarmProduceContract.Commands.CreateFarmProduce())
                this `fails with` "A newly created FarmProduce must have a positive weight."
            }
            // this transaction should pass
            transaction {
                output(FarmProduceContract.FarmProduce_ContractID, passingState)
                command(listOf(farmer1.publicKey), FarmProduceContract.Commands.CreateFarmProduce())
                verifies()
            }
        }
    }

    /**
     *New farm produce transactions should  have only positive minimum guaranteed amount.
     */
    @Test
    fun farmProduceMustHavePositiveMinguaranteedAmount() {
        val  passingState = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1.party)
        val  failingState = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((0.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1.party)


        ledgerServices.ledger {

            // Should fail since farm produce has minimum guaranteed amount 0
            transaction {
                output(FarmProduceContract.FarmProduce_ContractID, failingState)
                command(listOf(farmer1.publicKey), FarmProduceContract.Commands.CreateFarmProduce())
                this `fails with` "A newly created FarmProduce must have a positive minimum guaranteed amount."
            }
            // this transaction should pass
            transaction {
                output(FarmProduceContract.FarmProduce_ContractID, passingState)
                command(listOf(farmer1.publicKey), FarmProduceContract.Commands.CreateFarmProduce())
                verifies()
            }
        }
    }
}