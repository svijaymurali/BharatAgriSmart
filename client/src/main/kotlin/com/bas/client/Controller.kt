package com.bas.client

import net.corda.core.messaging.CordaRPCOps
import com.bas.flows.*
import com.bas.states.FarmAgreementState
import net.corda.core.utilities.OpaqueBytes
import com.bas.states.FarmProduceState
import net.corda.core.contracts.*
import net.corda.core.internal.toX500Name
import net.corda.core.node.NodeInfo
import org.bouncycastle.asn1.x500.style.BCStyle
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import sun.security.x509.X500Name
import java.time.format.DateTimeFormatter
import java.util.*
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.finance.contracts.Commodity
import net.corda.finance.contracts.Tenor
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.workflows.getCashBalances
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.GetMapping
import java.time.LocalDate
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria

/**
 * API endpoints for Bharat Agri Smart application
 */
val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 *  A Spring Boot `Server.kt` API controller for interacting with the node via RPC.
 */

@RestController
@RequestMapping("/api/bas/") // This is the base path for all other related requests
class Controller(rpc: NodeRPCConnection) {
    private val proxy = rpc.proxy
    private val me = proxy.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    fun X500Name.toDisplayString() : String  = BCStyle.INSTANCE.toString()

    /** Filters the node details from Network Map cache */
    private fun isNotary(nodeInfo: NodeInfo) = proxy.notaryIdentities().any { nodeInfo.isLegalIdentity(it) }
    private fun isMe(nodeInfo: NodeInfo) = nodeInfo.legalIdentities.first().name == me
    private fun isNetworkMap(nodeInfo : NodeInfo) = nodeInfo.legalIdentities.single().name.organisation == "Network Map Service"


    /**
     * Returns the node's name.
     */
    @GetMapping(value = ["me"], produces = [APPLICATION_JSON_VALUE])
    fun whoami() = mapOf("me" to me.toString())

    /**
     * To return all nodes registered with the [NetworkMapService]. We can use these names to find the identities
     * using [IdentityService].
     */
    @GetMapping(value = ["peers"], produces = [APPLICATION_JSON_VALUE])
    fun getPeers(): Map<String, List<String>> {
        return mapOf("peers" to proxy.networkMapSnapshot()
                .filter { isNotary(it).not() && isMe(it).not() && isNetworkMap(it).not() }
                .map { it.legalIdentities.first().name.toX500Name().toString() })
    }

    /**
     *
     * Displays all Farm produce states that exist in the node's vault.
     * TODO: Return a list of Farmproduce States on ledger
     */

    // To get only unconsumed states by default
    @GetMapping(value = ["farm-produce-states"], produces = [APPLICATION_JSON_VALUE])
    private fun getFarmproduces() = proxy.vaultQueryBy<FarmProduceState>().states.toString()

    // To filter unconsumed farm produce states
    @GetMapping(value = ["filter-farm-produce"], produces = [APPLICATION_JSON_VALUE])
    private fun filterFarmproduces( @RequestParam(value = "farmProduceToSearch") farmProduceToSearch: String
    ) = proxy.vaultQuery(FarmProduceState::class.java).states
        .filter { (state)  -> state.data.farmProduce == Commodity(farmProduceToSearch,farmProduceToSearch,0)}.toString()


    // To get all states from vault
    @GetMapping(value = ["farm-produce-states-all"], produces = [APPLICATION_JSON_VALUE])
    private fun getFarmproducestates() = proxy.vaultQueryBy<FarmProduceState>(criteria = QueryCriteria.VaultQueryCriteria(status =
    Vault.StateStatus.ALL)).states.toString()

    // To get metadata of states from vault
    @GetMapping(value = ["farm-produce-states-meta"], produces = [APPLICATION_JSON_VALUE])
    private fun getFarmproducestatesmeta() = proxy.vaultQueryBy<FarmProduceState>(criteria = QueryCriteria.VaultQueryCriteria(status =
    Vault.StateStatus.ALL)).statesMetadata.toString()

    /**
     *
     * Displays all Farm agreement states that exist in the node's vault.
     * TODO: Return a list of Farm agreements States on ledger

     */
    // To get only unconsumed states by default
    @GetMapping(value = ["farmagreements"], produces = [APPLICATION_JSON_VALUE])
    private fun getFarmagreements() = proxy.vaultQueryBy<FarmAgreementState>().states.toString()

    // To get all states from vault
    @GetMapping(value = ["farmagreements-all"], produces = [APPLICATION_JSON_VALUE])
    private fun getFarmagreementsstates() = proxy.vaultQueryBy<FarmAgreementState>(criteria = QueryCriteria.VaultQueryCriteria(status =
    Vault.StateStatus.ALL)).states.toString()


    // To get metadata of states from vault
    @GetMapping(value = ["farmagreements-meta"], produces = [APPLICATION_JSON_VALUE])
    private fun getFarmagreementsstatesmeta() = proxy.vaultQueryBy<FarmAgreementState>(criteria = QueryCriteria.VaultQueryCriteria(status =
    Vault.StateStatus.ALL)).statesMetadata.toString()

    /**
     * Displays all cash states that exist in the node's vault.
     */

    @GetMapping(value = ["cash"], produces = arrayOf("text/plain"))
    private fun getCash() = proxy.vaultQueryBy<Cash.State>().states.toString()

    /**
     * Displays cash balances that exist in the node's vault.
     */
    @GetMapping(value = ["cash-balance"], produces = [APPLICATION_JSON_VALUE])
    // Display cash balances.
    fun getCashBalances() = proxy.getCashBalances()

    /**
     * API endpoint to issue some cash to ourselves.
     *
     */
    @GetMapping(value = ["issue-cash-toself"], produces = [APPLICATION_JSON_VALUE])
    fun issueCashtoSelf(@RequestParam(value = "amount") amount: Int,
                        @RequestParam(value = "currency") currency: String): ResponseEntity<String> {
        val issueAmount = Amount(amount.toLong()*100, Currency.getInstance(currency))
        val notary = proxy.notaryIdentities().firstOrNull() ?: throw IllegalStateException("Could not find a notary.")
        val issueRef = OpaqueBytes.of(0)
        val issueRequest = CashIssueFlow.IssueRequest(issueAmount, issueRef, notary)
        return try {

            val cashState = proxy.startFlow(::CashIssueFlow, issueRequest).returnValue.get()

            ResponseEntity.status(HttpStatus.CREATED).body(cashState.toString())

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    /**
     * Creates new farm produce.
     */

    @PutMapping(value = ["create-farmproduce"], produces = [APPLICATION_JSON_VALUE])
    fun createFarmproducefunction(
            @RequestParam(value = "farmProduce") farmProduce: String, //Commodity
            @RequestParam(value = "weightInKG") weightInKG: Int,
            @RequestParam(value = "minGuaranteedPrice") minGuaranteedPrice: Double, //Amount<Currency>
            @RequestParam(value = "currency") currency: String, //Amount<Currency>
            @RequestParam(value = "expectedDelivery") expectedDelivery: String, //Date
            @RequestParam(value = "landAddress") landAddress: String,
            @RequestParam(value = "isCropInsured") isCropInsured: String //Boolean

    ): ResponseEntity<String> {
        // Get party objects for myself and the counterparty.
        val me = proxy.nodeInfo().legalIdentities.first()
        val farmProduces = Commodity(farmProduce, farmProduce, 0)
        val insurance = isCropInsured.toBoolean()
        val minGuartdPrice = Amount(minGuaranteedPrice.toLong()*100, Currency.getInstance(currency))
        val expDate = LocalDate.parse(expectedDelivery, DateTimeFormatter.ISO_DATE)

        try {
            val state= FarmProduceState(UniqueIdentifier(),farmProduces, weightInKG,minGuartdPrice,expDate,landAddress,insurance,me)
            val result = proxy.startFlowDynamic(CreateFarmProduceFlow::class.java, state).returnValue.get()
            //  proxy.startTrackedFlow(::CreateFarmProduceFlow, state).returnValue.get()
           // proxy.startFlowDynamic(CreateFarmProduceFlow::class.java, farmProduces, weightInKG,minGuartdPrice,expDate,landAddress,insurance,me)
            return ResponseEntity.status(HttpStatus.CREATED).body("Transaction for new Farm Produce with id ${result.id} is committed to ledger.\n${result.tx.outputs.single()}")

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }



    /** New Farm Agreement proposal.
     */

    @PutMapping(value = ["propose-agreement"], produces = [APPLICATION_JSON_VALUE] )
    //   @RequestMapping (method = [RequestMethod.PUT])
    fun proposeFarmAgreement(

            @RequestParam(value = "farmProduceReferenceID") farmProduceReferenceID: String, //UUID
        //    @RequestParam(value = "farmProduce") farmProduce: String, //Commodity
         //   @RequestParam(value = "weightInKG") weightInKG: Int,
       //     @RequestParam(value = "minGuaranteedPrice") minGuaranteedPrice: Double, //Amount<Currency>
        //    @RequestParam(value = "currencyprice") currencyprice: String, //Amount<Currency>
            @RequestParam(value = "agreedAmount") agreedAmount: Double,
            @RequestParam(value = "currency") currency: String, //Amount<Currency>
         //   @RequestParam(value = "buyer") buyer: String, // Party
       //     @RequestParam(value = "seller") seller: String, // Party
            @RequestParam(value = "durationOfAgreement") durationOfAgreement: String, // Tenor
            @RequestParam(value = "bonusCondition") bonusCondition: String,
            @RequestParam(value = "filePath") filePath: String

    ): ResponseEntity<String> {
        // Get party objects for myself.
        val me = proxy.nodeInfo().legalIdentities.first()
        val farmPrdUniqueID = UniqueIdentifier.Companion.fromString(farmProduceReferenceID)
     //   val farmProduces = Commodity(farmProduce, farmProduce, 0)
    //    val minGuartdPrice = Amount(minGuaranteedPrice.toLong()*100, Currency.getInstance(currency))
        val amountAgreed = Amount(agreedAmount.toLong()*100, Currency.getInstance(currency))
     //   val sellerID = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(seller)) ?: throw IllegalArgumentException("Unknown party name.")
        //val tenorofAgreement = LocalDateTime.parse(durationOfAgreement, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
        val tenor = Tenor(durationOfAgreement)


        try {
          //  val state = FarmAgreementState(farmPrdUniqueID,UniqueIdentifier(),farmProduces,weightInKG,minGuartdPrice,amountAgreed,me,sellerID,tenor,bonusCondition)
           // val result = proxy.startFlowDynamic(ProposeFarmAgreementFlow.Initiator::class.java,farmPrdUniqueID,farmProduces,weightInKG,minGuartdPrice,amountAgreed,sellerID,tenor,bonusCondition,filePath).returnValue.get()
            val result = proxy.startFlowDynamic(ProposeFarmAgreementFlow.Initiator::class.java,farmPrdUniqueID,amountAgreed,tenor,bonusCondition,filePath).returnValue.get()

            //val state = sellerID?.let { FarmAgreementState(UniqueIdentifier(), farmProduces, amtGMGP, me, it, weightInKG, tenor, bonusCondition) }
            //proxy.startTrackedFlow(::ProposeAgreementFlow,state).returnValue.get()
            //val result= flowHa.returnValue.get()
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction for new Farm agreement proposal with id ${result.id} is committed to ledger.${result.tx.outputs.single()}")

        } catch (e: Exception) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    /**
     * Settle Farm Agreement.
     */

    @GetMapping(value = ["settle-agreement"], produces = [APPLICATION_JSON_VALUE])
    fun settleFarmAgreement(
            @RequestParam(value = "farmAgreementID") farmAgreementID: String,
            @RequestParam(value = "settlementAmount") settlementAmount: Double,
            @RequestParam(value = "currency") currency: String //Amount<Currency>

            ): ResponseEntity<String> {

        val amountAgreed = Amount(settlementAmount.toLong()*100, Currency.getInstance(currency))
        val agreement = UniqueIdentifier.Companion.fromString(farmAgreementID)

        return try {
            proxy.startFlowDynamic(AgreementSettlementFlow::class.java, agreement,amountAgreed).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Farm Agreement with id $agreement settled with amount $amountAgreed")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }

    }


    /**
     * Accept Farm Agreement.
     */

    @GetMapping(value = ["accept-agreement"], produces = [APPLICATION_JSON_VALUE])
    fun accpetFarmAgreement(
            @RequestParam(value = "farmAgreementID") farmAgreementID: String

    ): ResponseEntity<String> {
        val me = proxy.nodeInfo().legalIdentities.first()
        val agreement = UniqueIdentifier.Companion.fromString(farmAgreementID)

        return try {
            proxy.startFlowDynamic(AcceptFarmAgreementFlow::class.java, agreement).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Farm Agreement with id $agreement is accepted by $me")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }

    }

    @GetMapping(value = ["cancel-agreement"], produces = [APPLICATION_JSON_VALUE])
    fun cancelFarmAgreement(
            @RequestParam(value = "farmAgreementID") farmAgreementID: String

    ): ResponseEntity<String> {
        val me = proxy.nodeInfo().legalIdentities.first()
        val agreement = UniqueIdentifier.Companion.fromString(farmAgreementID)

        return try {
            proxy.startFlowDynamic(CancelAgreementFlow::class.java, agreement).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Farm Agreement with id $agreement is cancelled by $me")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }

    }






    /*
    @GetMapping(value = ["transfer-farmproduce"], produces = [APPLICATION_JSON_VALUE])
    fun transferFarmproduce(
            @RequestParam(value = "id") id: String,
            @RequestParam(value = "party") party: String
    ): ResponseEntity<String> {
        // Get linearID and buyer identity to transfer farm produce
        val linearId = UniqueIdentifier.fromString(id)
        val newOwner = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party))
                ?: throw IllegalArgumentException("Unknown party name.")



        return try {
            proxy.startFlow(::TransferFarmProduceFlow, linearId, newOwner).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Farm Produce with $id transferred to $party.")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }

    }   */


}

