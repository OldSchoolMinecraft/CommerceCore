plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("libs/poseidon.jar"))
    implementation(files("libs/Essentials.jar"))
    implementation(files("libs/OSM-Ess.jar"))
    implementation(files("libs/ChestShop.jar"))

    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
    implementation("org.apache.poi:poi:5.5.1")
}