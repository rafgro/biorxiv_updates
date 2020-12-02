package com.rafgro.biorxivupdates

class StateOfFeed {
    var message : String = " "
    var progress : Int = 0
    var state : SpecificStateOfFeed = SpecificStateOfFeed.EMPTY

    constructor( pstate : SpecificStateOfFeed, pmessage : String = " " ) {
        state = pstate
        message = pmessage
    }
}

enum class SpecificStateOfFeed {
    EMPTY, LOADING_GENERAL, LOADING_DB, LOADING_NET, LOADED, FAILED
}