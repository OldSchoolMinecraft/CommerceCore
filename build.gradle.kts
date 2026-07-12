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
}