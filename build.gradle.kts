import korlibs.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
}

korge {
    id = "io.github.rezmike.game2048"
    name = "2048"
    icon = file("src/commonMain/resources/korge.png")

    // To enable all targets at once
	//targetAll()

    // To enable targets based on properties/environment variables
	//targetDefault()

    // To selectively enable targets
	targetJvm()
	targetJs()
    //targetWasmJs()
	//targetDesktop()
	//targetIos()
	//targetAndroid()

	serializationJson()
}

dependencies {
    add("commonMainApi", project(":deps"))
    //add("commonMainApi", project(":korge-dragonbones"))
}
