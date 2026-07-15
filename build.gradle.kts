plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/poseidon.jar"))
    compileOnly(files("libs/Essentials.jar"))
    compileOnly(files("libs/OSM-Ess.jar"))
    compileOnly(files("libs/ChestShop.jar"))

    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
    implementation("org.apache.poi:poi:5.5.1")
}