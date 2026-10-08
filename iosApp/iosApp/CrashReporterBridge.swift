import FirebaseCrashlytics
import ComposeApp

final class CrashReporterBridge: NSObject, IosCrashReporterBridge {

    func setUserId(userId: String?) {
        Crashlytics.crashlytics().setUserID(userId ?? "")
    }

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func recordException(name: String, reason: String?, stackTrace: String) {
        let crashlytics = Crashlytics.crashlytics()
        crashlytics.log(stackTrace)
        let error = NSError(
            domain: name,
            code: 0,
            userInfo: [NSLocalizedDescriptionKey: reason ?? name]
        )
        crashlytics.record(error: error)
    }
}
