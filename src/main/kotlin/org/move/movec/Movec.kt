package org.move.movec

//class Movec(private val executable: Path) {
//    @Throws(ExecutionException::class)
//    private fun fetchMetadata(owner: Project, projectDirectory: Path): MovecMetadata.Project {
//        val json = MovecCommandLine(executable, "metadata", projectDirectory)
//            .execute(owner)
//            .stdout
//            .dropWhile { it != '{' }
//        val project = try {
//            Gson().fromJson(json, MovecMetadata.Project::class.java)
//        } catch (e: JsonSyntaxException) {
//            throw ExecutionException(e)
//        }
//        return project
//    }
//}