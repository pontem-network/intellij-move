package org.move.movec.cli

import java.nio.file.Path

data class MovecProjectMetadata(
    // path to stdlib di
    val stdlib: Path,
    // list of paths to module dirs
    val modules: List<Path>,
    // "libra" or "dfinance"
    val network: String,
    // 0x.. or wallet1.. address
    val account_address: String,
)