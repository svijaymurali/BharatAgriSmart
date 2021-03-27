package com.bas.states

import com.bas.contracts.FarmAgreementContract
import com.bas.states.FarmAgreementStatus
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.finance.contracts.Commodity
import net.corda.finance.contracts.Tenor
import java.time.Instant
import java.time.LocalDate
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(FarmAgreementContract::class)
data class FarmAgreementState(
        val farmProduceItem: LinearPointer<LinearState>?,
       // val farmAgreementID: UUID,
  //      val farmProduceReferenceID: UniqueIdentifier,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        val farmProduce: Commodity,
        val weightInKG: Int,
        val minGuaranteedPrice: Amount<Currency>,
        val agreedAmount: Amount<Currency>,
        val expectedDelivery: LocalDate,
        val landAddress: String,
        val isCropInsured: Boolean,
        val buyer: Party,
        val seller: Party,
        val durationOfAgreement: Tenor?,
        val bonusCondition: String,
        val legalProseAttachmentHash: String,
        val status: FarmAgreementStatus = FarmAgreementStatus.IN_PROPOSAL
        ) :  LinearState  {
    override val participants: List<Party> get() = listOf(buyer,seller)

    /*private val duration = Instant.now().plusSeconds(300)
    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {

        val flowLogicRef = flowLogicRefFactory.create("com.bas.flows.EndAgreementFlow",duration )
        return ScheduledActivity(flowLogicRef,duration!!)
    }  */


    }



