package com.bas


import com.bas.flows.CreateFarmProduceFlow
import com.bas.flows.ProposeFarmAgreementFlow
import com.bas.states.FarmAgreementState
import com.bas.states.FarmAgreementStatus
import com.bas.states.FarmProduceState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.Vault
import net.corda.core.utilities.getOrThrow
import net.corda.finance.contracts.Commodity
import net.corda.finance.contracts.Tenor
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Future
import net.corda.testing.node.User
import kotlin.test.assertEquals

class NodeDriverTest {
    private val farmer1 = TestIdentity(CordaX500Name("FarmerA", "", "IN"))
    private val farmer2 = TestIdentity(CordaX500Name("FarmerB", "", "US"))
    private val farmer3 = TestIdentity(CordaX500Name("Farmer3", "", "DE"))

    private val orgMarket1 = TestIdentity(CordaX500Name("OrgMarketA", "", "GB"))
    private val orgMarket2 = TestIdentity(CordaX500Name("OrgMarketB", "", "DE"))


    // Test is run within Driver DSL
 private fun withDriver(test: DriverDSL.() -> Unit) = driver(
         DriverParameters(isDebug = true, startNodesInProcess = true,networkParameters = testNetworkParameters(minimumPlatformVersion = 4))
 ) { test() }

    // List will collect all asynchronous computation results by waiting for its exectuion
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Function to make RPC call
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name


    /*

    // All nodes are started in parallel
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
            .map { startNode(providedName = it.name) }
            .waitForAll()
     */

    @Test
    fun `testNodes`() = withDriver {

        // Set up users with Permissions
        val farmer1User = User("farmer1User", "", permissions = setOf(
              //  Permissions.startFlow<CreateFarmProduceFlow>(),
                //Permissions.startFlow<TransactionBroadcastFlow>(),
                //Permissions.invokeRpc("vaultTrackBy"),
                ("ALL")
        ))

        val farmer2User = User("farmer2User", "", permissions = setOf(
               // Permissions.startFlow<CreateFarmProduceFlow>(),
               // Permissions.startFlow<TransactionBroadcastFlow>(),
               // Permissions.invokeRpc("vaultTrackBy"),
                ("ALL")
        ))
        val farmer3User = User("farmer3User", "", permissions = setOf(
                // Permissions.startFlow<CreateFarmProduceFlow>(),
                // Permissions.startFlow<TransactionBroadcastFlow>(),
                // Permissions.invokeRpc("vaultTrackBy"),
                ("ALL")
        ))
        val orgMarket1User = User("orgMarket1User", "", permissions = setOf(
               // Permissions.startFlow<ProposeFarmAgreementFlow.Initiator>(),
               // Permissions.startFlow<AgreementSettlementFlow>(),
               // Permissions.invokeRpc("vaultTrackBy"),
                ("ALL")
        ))
        val orgMarket2User = User("orgMarket2User", "", permissions = setOf(
               // Permissions.startFlow<ProposeFarmAgreementFlow.Initiator>(),
               // Permissions.startFlow<AgreementSettlementFlow>(),
               // Permissions.invokeRpc("vaultTrackBy"),
                ("ALL")
        ))

        // Following 5 nodes are started
        val (farmer1Handle, farmer2Handle,farmer3Handle, orgMarket1Handle, orgMarket2Handle) =
        listOf(
                startNode(providedName = farmer1.name, rpcUsers = listOf(farmer1User)),
                startNode(providedName = farmer2.name, rpcUsers = listOf(farmer2User)),
                startNode(providedName = farmer3.name, rpcUsers = listOf(farmer3User)),
                startNode(providedName = orgMarket1.name, rpcUsers = listOf(orgMarket1User)),
                startNode(providedName = orgMarket2.name, rpcUsers = listOf(orgMarket2User))

                ).map { it.getOrThrow() }
        

        // RPC client and proxies are crated for each node
        val farmer1Client = CordaRPCClient(farmer1Handle.rpcAddress)
        val farmer1Proxy: CordaRPCOps = farmer1Client.start("farmer1User", "").proxy

        val farmer2Client = CordaRPCClient(farmer2Handle.rpcAddress)
        val farmer2Proxy: CordaRPCOps = farmer2Client.start("farmer2User", "").proxy

        val farmer3Client = CordaRPCClient(farmer3Handle.rpcAddress)
        val farmer3Proxy: CordaRPCOps = farmer3Client.start("farmer3User", "").proxy

        val orgMarket1Client = CordaRPCClient(orgMarket1Handle.rpcAddress)
        val orgMarket1Proxy: CordaRPCOps = orgMarket1Client.start("orgMarket1User", "").proxy

        val orgMarket2Client = CordaRPCClient(orgMarket2Handle.rpcAddress)
        val orgMarket2Proxy: CordaRPCOps = orgMarket2Client.start("orgMarket2User", "").proxy

        // Farm Produce flow is started

         val farmProduceState1 = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1Proxy.nodeInfo().legalIdentities.first())
         val farmProduceState2 = FarmProduceState(UniqueIdentifier(), Commodity("PA","Paddy",0),2000, Amount((2000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2022-12-11", DateTimeFormatter.ISO_DATE),"USA",equals("false"),farmer2Proxy.nodeInfo().legalIdentities.first())
         val farmProduceState3 = FarmProduceState(UniqueIdentifier(), Commodity("TO","Tomato",0),5000, Amount((4000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-09-11", DateTimeFormatter.ISO_DATE),"Frankfurt",equals("true"),farmer3Proxy.nodeInfo().legalIdentities.first())

        // Farm produce transaction is created
         val farmProduce1Tx  = farmer1Handle.rpc.startFlow(::CreateFarmProduceFlow, farmProduceState1).returnValue.getOrThrow()
         val farmProduce2Tx  = farmer2Handle.rpc.startFlow(::CreateFarmProduceFlow, farmProduceState2).returnValue.getOrThrow()
         val farmProduce3Tx  = farmer3Handle.rpc.startFlow(::CreateFarmProduceFlow, farmProduceState3).returnValue.getOrThrow()


        // Farm produce's unique linear id is collected to pass on to Farm Agreement later
        val farmProduce1State = farmProduce1Tx.tx.outputs.map { it.data }.filterIsInstance<FarmProduceState>().single()
        val farmProduce2State = farmProduce2Tx.tx.outputs.map { it.data }.filterIsInstance<FarmProduceState>().single()
      //  val farmProduce3linearID = farmProduce3Tx.tx.outputs.map { it.data }.filterIsInstance<FarmProduceState>().single().linearId

        val farmProduce3State = farmProduce3Tx.tx.outputs.map { it.data }.filterIsInstance<FarmProduceState>().single()



        // Check is performed to assert Farmer 1-3's vault is updated as expected
        assertEquals("India",farmProduce1Tx.tx.outputs.map { it.data }.filterIsInstance<FarmProduceState>().single().landAddress)
        assertEquals("USA",farmProduce2Tx.tx.outputs.map { it.data }.filterIsInstance<FarmProduceState>().single().landAddress)
        assertEquals("Frankfurt",farmProduce3Tx.tx.outputs.map { it.data }.filterIsInstance<FarmProduceState>().single().landAddress)


        // Farm Agreement transaction is created
        //val farmAgreement1Tx  = orgMarket1Handle.rpc.startFlowDynamic(ProposeFarmAgreementFlow.Initiator::class.java,farmProduce1linearID, Commodity("WT","Wheat",0),3000, Amount.parseCurrency("3000 INR"),Amount.parseCurrency("4500 INR"),farmer1Proxy.nodeInfo().legalIdentities.first(), Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip").returnValue.getOrThrow()

        val farmAgreement1Tx  = orgMarket2Handle.rpc.startFlowDynamic(ProposeFarmAgreementFlow.Initiator::class.java,farmProduce1State.linearId,Amount.parseCurrency("4500 INR"),Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip").returnValue.getOrThrow()
               // farmProduce1State.farmProduce,farmProduce1State.weightInKG,farmProduce1State.minGuaranteedPrice,Amount.parseCurrency("4500 INR"),farmProduce1State.expectedDelivery,farmProduce1State.landAddress,farmProduce1State.isCropInsured,farmProduce1State.owner, Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip").returnValue.getOrThrow()

        // val farmAgreement2Tx  = orgMarket2Handle.rpc.startFlowDynamic(ProposeFarmAgreementFlow.Initiator::class.java,farmProduce2linearID, Commodity("PA","Paddy",0),2000, Amount.parseCurrency("2000 INR"),Amount.parseCurrency("3500 INR"),farmer2Proxy.nodeInfo().legalIdentities.first(), Tenor("2Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose1.zip").returnValue.getOrThrow()
        val farmAgreement2Tx  = orgMarket2Handle.rpc.startFlowDynamic(ProposeFarmAgreementFlow.Initiator::class.java,farmProduce2State.linearId, Amount.parseCurrency("3500 INR"),Tenor("2Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose1.zip").returnValue.getOrThrow()

        //farmProduce2State.farmProduce,farmProduce2State.weightInKG,farmProduce2State.minGuaranteedPrice,Amount.parseCurrency("3500 INR"),farmProduce2State.expectedDelivery,farmProduce2State.landAddress,farmProduce2State.isCropInsured,farmProduce2State.owner, Tenor("2Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose1.zip").returnValue.getOrThrow()

        val farmAgreement3Tx  = orgMarket2Handle.rpc.startFlowDynamic(ProposeFarmAgreementFlow.Initiator::class.java,farmProduce3State.linearId,Amount.parseCurrency("5500 INR"),Tenor("1Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose2.zip").returnValue.getOrThrow()
              //  farmProduce3State.farmProduce,farmProduce3State.weightInKG,farmProduce3State.minGuaranteedPrice,Amount.parseCurrency("5500 INR"),farmProduce3State.expectedDelivery,farmProduce3State.landAddress,farmProduce3State.isCropInsured,farmProduce3State.owner, Tenor("1Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose2.zip").returnValue.getOrThrow()

        //Commodity("TO","Tomato",0),5000, Amount.parseCurrency("4000 INR"),Amount.parseCurrency("5500 INR"),farmer3Proxy.nodeInfo().legalIdentities.first(), Tenor("1Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose2.zip").returnValue.getOrThrow()

        // Check is performed to assert Farmer 1-3's vault is updated as expected
        assertEquals("3Y",farmAgreement1Tx.tx.outputs.map { it.data }.filterIsInstance<FarmAgreementState>().single().durationOfAgreement.toString())
        assertEquals("2Y",farmAgreement2Tx.tx.outputs.map { it.data }.filterIsInstance<FarmAgreementState>().single().durationOfAgreement.toString())
        assertEquals("1Y",farmAgreement3Tx.tx.outputs.map { it.data }.filterIsInstance<FarmAgreementState>().single().durationOfAgreement.toString())

    }


}


