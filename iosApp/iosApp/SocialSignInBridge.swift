import AuthenticationServices
import CryptoKit
import FirebaseCore
import GoogleSignIn
import UIKit
import ComposeApp

/// Swift implementation of the shared `IosSocialSignInBridge` (defined in `iosMain`). It drives the
/// native Google and Apple account pickers and hands the resulting tokens back to Kotlin, which
/// links them onto the current Firebase account (SS-061 / SS-087).
final class SocialSignInBridge: NSObject, IosSocialSignInBridge {

    // ASAuthorizationController does not retain its delegate, so the in-flight Apple coordinator is
    // held here until it completes.
    private var appleCoordinator: AppleSignInCoordinator?

    func signInWithGoogle(onResult: @escaping (IosGoogleSignInResult) -> Void) {
        // The Google client id only exists once Google auth is enabled in the console and a fresh
        // GoogleService-Info.plist is added (SS-088); until then, no-op like a cancel.
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            onResult(IosGoogleSignInResult(idToken: nil, accessToken: nil, isCancelled: true))
            return
        }
        guard let presenter = Self.topViewController() else {
            onResult(IosGoogleSignInResult(idToken: nil, accessToken: nil, isCancelled: false))
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { signInResult, error in
            if let error = error {
                let isCancelled = (error as NSError).code == GIDSignInError.canceled.rawValue
                onResult(
                    IosGoogleSignInResult(idToken: nil, accessToken: nil, isCancelled: isCancelled)
                )
                return
            }

            guard let user = signInResult?.user, let idToken = user.idToken?.tokenString else {
                onResult(IosGoogleSignInResult(idToken: nil, accessToken: nil, isCancelled: false))
                return
            }

            onResult(
                IosGoogleSignInResult(
                    idToken: idToken,
                    accessToken: user.accessToken.tokenString,
                    isCancelled: false
                )
            )
        }
    }

    func signInWithApple(onResult: @escaping (IosAppleSignInResult) -> Void) {
        let coordinator = AppleSignInCoordinator { [weak self] result in
            self?.appleCoordinator = nil
            onResult(result)
        }
        appleCoordinator = coordinator
        coordinator.start()
    }

    private static func topViewController() -> UIViewController? {
        var top = Self.keyWindow()?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }

    fileprivate static func keyWindow() -> UIWindow? {
        let scene =
            UIApplication.shared.connectedScenes
                .first { $0.activationState == .foregroundActive } as? UIWindowScene
        return scene?.windows.first { $0.isKeyWindow }
    }
}

/// Runs one Apple sign-in request: generates a nonce, sends its SHA-256 hash to Apple, and returns
/// the identity token plus the *raw* nonce (Firebase verifies the token against it).
private final class AppleSignInCoordinator: NSObject, ASAuthorizationControllerDelegate,
    ASAuthorizationControllerPresentationContextProviding {

    private let onResult: (IosAppleSignInResult) -> Void
    private let rawNonce: String

    init(onResult: @escaping (IosAppleSignInResult) -> Void) {
        self.onResult = onResult
        self.rawNonce = Self.randomNonceString()
    }

    func start() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(rawNonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard
            let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
            let tokenData = credential.identityToken,
            let idToken = String(data: tokenData, encoding: .utf8)
        else {
            onResult(IosAppleSignInResult(idToken: nil, rawNonce: nil, isCancelled: false))
            return
        }
        onResult(IosAppleSignInResult(idToken: idToken, rawNonce: rawNonce, isCancelled: false))
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        let isCancelled = (error as? ASAuthorizationError)?.code == .canceled
        onResult(IosAppleSignInResult(idToken: nil, rawNonce: nil, isCancelled: isCancelled))
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        return SocialSignInBridge.keyWindow() ?? ASPresentationAnchor()
    }

    private static func sha256(_ input: String) -> String {
        return SHA256.hash(data: Data(input.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }

    private static func randomNonceString(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remaining = length

        while remaining > 0 {
            var randomBytes = [UInt8](repeating: 0, count: 16)
            let status = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
            guard status == errSecSuccess else {
                fatalError("Unable to generate a secure nonce (SecRandomCopyBytes failed: \(status))")
            }

            for byte in randomBytes where remaining > 0 {
                if byte < charset.count {
                    result.append(charset[Int(byte)])
                    remaining -= 1
                }
            }
        }
        return result
    }
}
