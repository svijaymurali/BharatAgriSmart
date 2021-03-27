package com.bas.contracts


import com.bas.states.FarmProduceState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.finance.contracts.Commodity
import org.junit.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals


class FarmProduceStateTest {

    @Test
    fun fieldTypeTest() {

        // Checks whether Farm Produce state fields exist?
        FarmProduceState::class.java.getDeclaredField("farmProduce")
        FarmProduceState::class.java.getDeclaredField("weightInKG")
        FarmProduceState::class.java.getDeclaredField("minGuaranteedPrice")
        FarmProduceState::class.java.getDeclaredField("expectedDelivery")
        FarmProduceState::class.java.getDeclaredField("landAddress")
        FarmProduceState::class.java.getDeclaredField("isCropInsured")
        FarmProduceState::class.java.getDeclaredField("owner")

        // To check the data type of Farm Produce state fields?
        assertEquals(FarmProduceState::class.java.getDeclaredField("farmProduce").type, Commodity::class.java)
        assertEquals(FarmProduceState::class.java.getDeclaredField("weightInKG").type, Int::class.java)
        assertEquals(FarmProduceState::class.java.getDeclaredField("minGuaranteedPrice").type, Amount::class.java)
        assertEquals(FarmProduceState::class.java.getDeclaredField("expectedDelivery").type, LocalDate::class.java)
        assertEquals(FarmProduceState::class.java.getDeclaredField("landAddress").type, String::class.java)
        assertEquals(FarmProduceState::class.java.getDeclaredField("isCropInsured").type, Boolean::class.java)
        assertEquals(FarmProduceState::class.java.getDeclaredField("owner").type, AbstractParty::class.java)
    }
}


