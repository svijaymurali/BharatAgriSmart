package com.bas.states


import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class FarmAgreementStatus {
    IN_PROPOSAL,IN_CONTRACT,SETTLED,CANCELLED
}