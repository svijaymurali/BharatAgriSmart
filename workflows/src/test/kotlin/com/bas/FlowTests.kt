package com.bas

import com.bas.flows.*
import com.bas.states.*
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.contracts.Commodity
import net.corda.finance.contracts.Tenor
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutionException
import net.corda.testing.node.TestCordapp.Companion.findCordapp



class FlowTests {
    private lateinit var testNetwork: MockNetwork
    private lateinit var farmerNode1: StartedMockNode
    private lateinit var farmerNode2: StartedMockNode
    private lateinit var organicMarketNode: StartedMockNode
    private lateinit var notaryNode: StartedMockNode
    @Before
    fun setup() {

        testNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                findCordapp("com.bas.contracts"),
                findCordapp("com.bas.flows"),
                findCordapp("net.corda.finance.contracts.asset")

        ), networkParameters = testNetworkParameters(minimumPlatformVersion = 4)))

        farmerNode1 = testNetwork.createPartyNode()
        farmerNode2 = testNetwork.createPartyNode()
        organicMarketNode = testNetwork.createPartyNode()
        notaryNode = testNetwork.defaultNotaryNode
        testNetwork.runNetwork()

    }

    @After
    fun tearDown() {
        testNetwork.stopNodes()
    }

    /*
        Create Farm Produce flow test
    */
    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testFarmProduceCreateFlow() {
        val farmer1 = farmerNode1.info.chooseIdentityAndCert().party
        val farmProduceState = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1)
        val flow = CreateFarmProduceFlow(farmProduceState)
        val future = farmerNode1.startFlow(flow)
        testNetwork.runNetwork()
        val signedTransaction = future.get()
        val farmProduceStateOutput = signedTransaction.tx.getOutput(0) as FarmProduceState
        Assert.assertNotNull(farmProduceStateOutput)
    }

    /*
     Propose farm agreement flow test, first farm produce is created and then its Unique identifier used in
     farm agreement's Linear pointer
    */
    @Test
    @Throws(Exception::class)
    fun testProposeFarmAgreementFlow() {

        val farmer1 = farmerNode1.info.chooseIdentityAndCert().party
        val farmProduceState = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1)
        val flow = CreateFarmProduceFlow(farmProduceState)
        val future = farmerNode1.startFlow(flow)
        testNetwork.runNetwork()
        val signedTransaction = future.get()
        val (linearId) = signedTransaction.tx.getOutput(0) as FarmProduceState
        val farmProduceStateOutput= signedTransaction.tx.getOutput(0) as FarmProduceState

        val orgMarket = organicMarketNode.info.chooseIdentityAndCert().party
        val farmAgreementFlow  = ProposeFarmAgreementFlow.Initiator(farmProduceStateOutput.linearId, Amount.parseCurrency("4500 INR"), Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip")

       // farmProduceStateOutput.farmProduce,farmProduceStateOutput.weightInKG,farmProduceStateOutput.minGuaranteedPrice,farmProduceStateOutput.expectedDelivery,farmProduceStateOutput.landAddress,farmProduceStateOutput.isCropInsured, farmProduceStateOutput.owner as Party,
         //       Commodity("WT","Wheat",0),3000, Amount.parseCurrency("3000 INR"),Amount.parseCurrency("4500 INR"),farmer1, Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip")
        val future1 = organicMarketNode.startFlow(farmAgreementFlow)
        testNetwork.runNetwork()
        val transaction = future1.get()
        val farmAgreementState = transaction.tx.getOutput(0) as FarmAgreementState
        Assert.assertNotNull(farmAgreementState)
    }

    /*
      Accept farm agreement flow test,first farm produce is created and then its linear id is pointed in farm
      agreement proposal. Then farm agreements unique identifier filtered from Farm Agreement State
       to accept the farm agreement
    */
    @Test
    @Throws(Exception::class)
    fun testAcceptFarmAgreementFlow() {

        val farmer1 = farmerNode1.info.chooseIdentityAndCert().party
        val orgMarket = organicMarketNode.info.chooseIdentityAndCert().party
        // Farm produce is created and its linear id is extracted
        val farmProduceState = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1)
        val flow = CreateFarmProduceFlow(farmProduceState)
        val future = farmerNode1.startFlow(flow)
        testNetwork.runNetwork()
        val signedTransaction = future.get()
        val (linearId) = signedTransaction.tx.getOutput(0) as FarmProduceState
        val farmProduceStateOutput= signedTransaction.tx.getOutput(0) as FarmProduceState

        //Farm agreement is proposed and its linear id extracted
       // val farmAgreementflow  = ProposeFarmAgreementFlow.Initiator(linearId, Commodity("WT","Wheat",0),3000, Amount.parseCurrency("3000 INR"),Amount.parseCurrency("4500 INR"),farmer1, Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip")

        val farmAgreementFlow  = ProposeFarmAgreementFlow.Initiator(farmProduceStateOutput.linearId, Amount.parseCurrency("4500 INR"), Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip")
        val future1 = organicMarketNode.startFlow<SignedTransaction>(farmAgreementFlow)
        testNetwork.runNetwork()
        val transaction = future1.get()
        val farmAgreementLinearId = transaction.tx.outputs.single().data as FarmAgreementState

        //Proposed farm agreement is accepted
        val acceptFlow = AcceptFarmAgreementFlow(farmAgreementLinearId.linearId)
        val future2 = farmerNode1.startFlow(acceptFlow)
        testNetwork.runNetwork()
        val signedTransaction1 = future2.get()
        val farmAgreement = signedTransaction1.tx.getOutput(0) as FarmAgreementState
        Assert.assertNotNull(farmAgreement)
    }

    /**
     * Self issuing on-ledger cash to organic market, before settling farm agreement
     */
    private fun selfIssueCash(amount: Amount<Currency>): Cash.State  {

        val notary =notaryNode.info.chooseIdentityAndCert().party
        val issueRef = OpaqueBytes.of(0)

        val cashIssueTx = (CashIssueFlow.IssueRequest(amount, issueRef, notary))
        val flow = CashIssueFlow(cashIssueTx)
        val future = organicMarketNode.startFlow(flow)
        testNetwork.runNetwork()
        val cashTransaction= future.get()
        return cashTransaction.stx.tx.outputs.single().data as Cash.State
    }


    /*
      Settle farm agreement flow test,first farm produce is created and then its linear id is pointed in farm
      agreement proposal. Then farm agreements unique identifier filtered from Farm Agreement State
      to settle the farm agreement with agreed amount.
      We also use Corda's in-built cash module to issue cash to buyer before settling the contract.
     */
    @Test
    @Throws(Exception::class)
    fun testSettleFarmAgreementFlow() {

        val farmer1 = farmerNode1.info.chooseIdentityAndCert().party
        val orgMarket = organicMarketNode.info.chooseIdentityAndCert().party

        // Cash is issued to buyer before settling the farm agreement
        val future0 = selfIssueCash(Amount.parseCurrency("300000 INR"))
        testNetwork.runNetwork()
        val cashTx = future0.amount
        // Farm produce is created and its linear id is extracted
        val farmProduceState = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1)
        val flow = CreateFarmProduceFlow(farmProduceState)
        val future = farmerNode1.startFlow(flow)
        testNetwork.runNetwork()
        val signedTransaction = future.get()
        val (linearId) = signedTransaction.tx.getOutput(0) as FarmProduceState
        val farmProduceStateOutput= signedTransaction.tx.getOutput(0) as FarmProduceState

        //Farm agreement is proposed and its linear id extracted
        val farmAgreementFlow  = ProposeFarmAgreementFlow.Initiator(farmProduceStateOutput.linearId, Amount.parseCurrency("4500 INR"), Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip")
       // val farmAgreementflow  = ProposeFarmAgreementFlow.Initiator(linearId, Commodity("WT","Wheat",0),3000, Amount.parseCurrency("3000 INR"),Amount.parseCurrency("4500 INR"),farmer1, Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip")
        val future1 = organicMarketNode.startFlow<SignedTransaction>(farmAgreementFlow)
        testNetwork.runNetwork()
        val transaction = future1.get()
        val farmAgreementLinearId = transaction.tx.outputs.single().data as FarmAgreementState

        //Proposed farm agreement is accepted
        val acceptFlow = AcceptFarmAgreementFlow(farmAgreementLinearId.linearId)
        val future2 = farmerNode1.startFlow(acceptFlow)
        testNetwork.runNetwork()
        val acceptFlowsignedTransaction = future2.get()
        val acceptedFarmAgreement = acceptFlowsignedTransaction.tx.getOutput(0) as FarmAgreementState

        //Accepted farm agreement is settled

        val settleFlow = AgreementSettlementFlow(acceptedFarmAgreement.linearId,acceptedFarmAgreement.agreedAmount)
        val future3 = organicMarketNode.startFlow(settleFlow)
        testNetwork.runNetwork()
        val settleFlowsignedTransaction = future3.get()
        val settleFarmAgreement = settleFlowsignedTransaction.tx.getOutput(0)
        Assert.assertNotNull(settleFarmAgreement)
    }

    /*
    Cancel farm agreement flow test,first farm produce is created and then its linear id is pointed in farm
    agreement proposal. Then farm agreements unique identifier filtered from Farm Agreement State
    to accept the farm agreement. Accepted farm agreement can be cancelled with its unique identifier
    */
    @Test
    @Throws(Exception::class)
    fun testCancelFarmAgreementFlow() {

        val farmer1 = farmerNode1.info.chooseIdentityAndCert().party
        val orgMarket = organicMarketNode.info.chooseIdentityAndCert().party
        // Farm produce is created and its linear id is extracted
        val farmProduceState = FarmProduceState(UniqueIdentifier(), Commodity("WT","Wheat",0),3000, Amount((3000.toLong()), BigDecimal(100), Currency.getInstance("INR")), LocalDate.parse("2021-12-11", DateTimeFormatter.ISO_DATE),"India",equals("true"),farmer1)
        val flow = CreateFarmProduceFlow(farmProduceState)
        val future = farmerNode1.startFlow(flow)
        testNetwork.runNetwork()
        val signedTransaction = future.get()
        val (linearId) = signedTransaction.tx.getOutput(0) as FarmProduceState
        val farmProduceStateOutput= signedTransaction.tx.getOutput(0) as FarmProduceState

        //Farm agreement is proposed and its linear id extracted
        val farmAgreementFlow  = ProposeFarmAgreementFlow.Initiator(farmProduceStateOutput.linearId, Amount.parseCurrency("4500 INR"), Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip")
        //val farmAgreementflow  = ProposeFarmAgreementFlow.Initiator(linearId, Commodity("WT","Wheat",0),3000, Amount.parseCurrency("3000 INR"),Amount.parseCurrency("4500 INR"),farmer1, Tenor("3Y"),"3% of Market","C://Users//svijaymurali//BharatAgriSmart/workflows/legalprose.zip")
        val future1 = organicMarketNode.startFlow<SignedTransaction>(farmAgreementFlow)
        testNetwork.runNetwork()
        val transaction = future1.get()
        val farmAgreementLinearId = transaction.tx.outputs.single().data as FarmAgreementState

        //Proposed farm agreement is accepted
        val acceptFlow = AcceptFarmAgreementFlow(farmAgreementLinearId.linearId)
        val future2 = farmerNode1.startFlow(acceptFlow)
        testNetwork.runNetwork()
        val acceptFlowsignedTransaction = future2.get()
        val acceptedFarmAgreement = acceptFlowsignedTransaction.tx.getOutput(0) as FarmAgreementState

        //Accepted farm agreement is cancelled

        val cancelFlow = CancelAgreementFlow(acceptedFarmAgreement.linearId)
        val future3 = farmerNode1.startFlow(cancelFlow)
        testNetwork.runNetwork()
        val cancelFlowsignedTransaction = future3.get()
        val cancelFarmAgreement = cancelFlowsignedTransaction.tx.getOutput(0) as FarmAgreementState
        Assert.assertNotNull(cancelFarmAgreement)
    }


}