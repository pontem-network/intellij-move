package org.move.cli

import com.google.gson.annotations.SerializedName

data class GitDependency(
    val git: String,
)

data class LocalDependency(
    val path: String,
)

data class PackageInfo(
    val name: String,
    val account_address: String,
    val authors: List<String>,
    val local_dependencies: List<LocalDependency>,
    val git_dependencies: List<GitDependency>,
    val blockchain_api: String?,
)

data class LayoutInfo(
    val module_dir: String,
    val script_dir: String,
    val tests_dir: String,
    val module_output: String,
    val script_output: String,
    val target_deps: String,
    val target: String,
    val index: String,
)

data class DoveProjectMetadata(
    @SerializedName("package")
    val _package: PackageInfo,
    val layout: LayoutInfo,
)