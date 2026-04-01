import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.rikka.refine) apply false
}

subprojects {
    plugins.withId("com.android.application") {
        plugins.apply("dev.rikka.tools.refine")
        extensions.findByType(BaseExtension::class.java)?.apply {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
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
                exclude(group = "androidx.preference", module = "preference")
                exclude(group = "androidx.recyclerview", module = "recyclerview")
                exclude(group = "androidx.slidingpanelayout", module = "slidingpanelayout")
                exclude(group = "androidx.swiperefreshlayout", module = "swiperefreshlayout")
                exclude(group = "com.google.android.material", module = "material")
            }
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
