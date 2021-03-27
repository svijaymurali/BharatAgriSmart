package com.bas.contracts



import com.bas.states.FarmAgreementState
import com.bas.states.FarmAgreementStatus
import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearPointer
import net.corda.core.identity.Party
import net.corda.finance.contracts.Commodity
import net.corda.finance.contracts.Tenor
import org.junit.Test

import kotlin.test.assertEquals


class FarmAgreementStateTest {

    @Test
    fun fieldTypeTest() {

        // Checks whether Farm Produce state fields exist?
        FarmAgreementState::class.java.getDeclaredField("farmProduceItem")
        FarmAgreementState::class.java.getDeclaredField("farmProduce")
        FarmAgreementState::class.java.getDeclaredField("weightInKG")
        FarmAgreementState::class.java.getDeclaredField("minGuaranteedPrice")
        FarmAgreementState::class.java.getDeclaredField("agreedAmount")
        FarmAgreementState::class.java.getDeclaredField("buyer")
        FarmAgreementState::class.java.getDeclaredField("seller")
        FarmAgreementState::class.java.getDeclaredField("durationOfAgreement")
        FarmAgreementState::class.java.getDeclaredField("bonusCondition")
        FarmAgreementState::class.java.getDeclaredField("legalProseAttachmentHash")
     //   FarmAgreementState::class.java.getDeclaredField("isCropInsured")
        FarmAgreementState::class.java.getDeclaredField("status")

        // To check the data type of Farm Produce state fields?
        assertEquals(FarmAgreementState::class.java.getDeclaredField("farmProduceItem").type,LinearPointer::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("farmProduce").type, Commodity::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("weightInKG").type, Int::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("minGuaranteedPrice").type, Amount::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("agreedAmount").type, Amount::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("buyer").type, Party::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("seller").type, Party::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("durationOfAgreement").type, Tenor::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("bonusCondition").type, String::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("legalProseAttachmentHash").type, String::class.java)
        assertEquals(FarmAgreementState::class.java.getDeclaredField("status").type, FarmAgreementStatus::class.java)

    }
}
