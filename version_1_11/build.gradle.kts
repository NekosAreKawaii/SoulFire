plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.11-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
}