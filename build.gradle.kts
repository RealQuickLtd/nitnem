import com.android.build.api.dsl.CommonExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    plugins.withId("com.android.application") {
        val android = extensions.findByName("android")
        if (android is CommonExtension) {
            android.compileOptions.sourceCompatibility = JavaVersion.VERSION_21
            android.compileOptions.targetCompatibility = JavaVersion.VERSION_21
        }

        // SESL ships forks of these under its own groups; keeping the upstream
        // artifacts on the classpath duplicates classes and resources.
        configurations.all {
            exclude(group = "androidx.core", module = "core")
            exclude(group = "androidx.core", module = "core-ktx")
            exclude(group = "androidx.customview", module = "customview")
            exclude(group = "androidx.coordinatorlayout", module = "coordinatorlayout")
            exclude(group = "androidx.drawerlayout", module = "drawerlayout")
            exclude(group = "androidx.viewpager2", module = "viewpager2")
            exclude(group = "androidx.viewpager", module = "viewpager")
            exclude(group = "androidx.appcompat", module = "appcompat")
            exclude(group = "androidx.fragment", module = "fragment")
            exclude(group = "androidx.fragment", module = "fragment-ktx")
            exclude(group = "androidx.preference", module = "preference")
            exclude(group = "androidx.recyclerview", module = "recyclerview")
            exclude(group = "androidx.slidingpanelayout", module = "slidingpanelayout")
            exclude(group = "androidx.swiperefreshlayout", module = "swiperefreshlayout")
            exclude(group = "com.google.android.material", module = "material")
        }
    }
}
