package com.mf650.manager

import com.mf650.manager.data.parser.PadavanParsers
import com.mf650.manager.data.model.CellularStatus
import com.mf650.manager.data.model.LteCellInfo
import com.mf650.manager.data.model.NrCellInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryFailoverTest {

    @Test
    fun testFallbackCellularStatusParsing() {
        // Simulates Port 80 response when 8081 is down
        val port80Html = """
            sim_status = 1;
            net_type = "5G SA";
            rsrp_5g = -86;
            sinr_5g = 21;
            rsrq_5g = -10;
            rsrp_4g = -95;
            rsrq_4g = -13;
        """.trimIndent()

        val parsed = PadavanParsers.parseSystemStatus(port80Html)
        assertTrue(parsed.simCardReady)
        assertEquals("5G SA", parsed.networkType)

        // Construct fallback object as repository would
        val fallback = CellularStatus(
            status = 1,
            network = parsed.networkType,
            lte = LteCellInfo(rsrp = parsed.rsrp4g, rsrq = parsed.rsrq4g),
            nr = NrCellInfo(rsrp = parsed.rsrp5g, rsrq = parsed.rsrq5g)
        )

        assertEquals("5G SA", fallback.network)
        assertEquals(-86, fallback.nr?.rsrp)
        assertEquals(-95, fallback.lte?.rsrp)
    }
}
