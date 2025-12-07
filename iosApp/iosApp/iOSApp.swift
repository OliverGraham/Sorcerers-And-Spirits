import SwiftUI
import FirebaseCore
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        initialize()
        return true
    }

    private func initialize() {
        let tavernStateFlowContainer = StateFlowContainer<Tavern>()
        let iOSTavernRepository = IOSTavernRepositoryImplementation(stateFlowContainer: tavernStateFlowContainer)
        let tavernRepository = IOSTavernRepositoryCoordinator(
            stateFlowContainer: tavernStateFlowContainer,
            tavernRepositoryCopyForIOS: iOSTavernRepository
        )

        InitializeKoinKt.initializeKoin(
            authenticationRepository: IOSAuthenticationRepository(),
            tavernRepository: tavernRepository,
            travelerRepository: IOSTravelerRepository()
        )

        iOSTavernRepository.collectTavernFlow()
    }
}

@main
struct iOSApp: App {
    // Necessary for Koin, for some reason
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}