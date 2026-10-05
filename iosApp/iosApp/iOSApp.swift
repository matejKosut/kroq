import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        HealthKitManager.shared.startObservingSteps()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}