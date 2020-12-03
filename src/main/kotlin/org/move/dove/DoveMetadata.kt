package org.move.dove

object DoveMetadata {
    data class GitDependency(
        val git: String,
    )

    data class LocalDependency(
        val path: String,
    )

    data class Package(
        val name: String,
        val account_address: String,
        val authors: List<String>,
        val local_dependencies: List<LocalDependency>,
        val git_dependencies: List<GitDependency>,
        val blockchain_api: String?,
    )

    data class Layout(
        val module_dir: String,
        val script_dir: String,
        val tests_dir: String,
        val module_output: String,
        val script_output: String,
        val target_deps: String,
        val target: String,
        val index: String,
    )
}