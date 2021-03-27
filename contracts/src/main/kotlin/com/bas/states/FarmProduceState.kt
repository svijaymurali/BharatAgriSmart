package com.bas.states

import com.bas.contracts.FarmProduceContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.QueryableState
import net.corda.finance.contracts.Commodity
import java.time.LocalDate
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(FarmProduceContract::class)
data class FarmProduceState(
        override val linearId: UniqueIdentifier,
        val farmProduce: Commodity,
        val weightInKG: Int,
        val minGuaranteedPrice: Amount<Currency>,
        val expectedDelivery: LocalDate,
        val landAddress: String,
        val isCropInsured: Boolean,
        override val owner: AbstractParty
     //  val status: FarmProduceStatus = FarmProduceStatus.AVAILABLE
        ) : OwnableState, LinearState {
    override val participants: List<AbstractParty> get() = listOf(owner)
   override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(FarmProduceContract.Commands.TransferFarmProduce(), FarmProduceState(this.linearId, this.farmProduce, this.weightInKG,this.minGuaranteedPrice,this.expectedDelivery, this.landAddress,this.isCropInsured, newOwner))
    }
}