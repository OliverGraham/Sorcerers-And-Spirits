package com.linklabs.sorcerersandspirits.domain.authentication.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.authentication.SocialTokenResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class LinkAccountUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk()
    private val socialSignInProvider: SocialSignInProvider = mockk()

    private val useCase =
        LinkAccountUseCase(
            authenticationRepository = authenticationRepository,
            socialSignInProvider = socialSignInProvider,
        )

    @Test
    fun `Given a Google token is granted, when linking Google, then the repository links with that token`() =
        runTest {
            coEvery { socialSignInProvider.requestToken(method = SignInMethod.GOOGLE) } returns
                SocialTokenResult.Google(idToken = ID_TOKEN, accessToken = ACCESS_TOKEN)
            coEvery {
                authenticationRepository.linkWithGoogle(
                    idToken = ID_TOKEN,
                    accessToken = ACCESS_TOKEN,
                )
            } returns LinkAccountResult.SUCCESS

            val result = useCase(method = SignInMethod.GOOGLE)

            assertEquals(expected = LinkAccountResult.SUCCESS, actual = result)
            coVerify {
                authenticationRepository.linkWithGoogle(
                    idToken = ID_TOKEN,
                    accessToken = ACCESS_TOKEN,
                )
            }
        }

    @Test
    fun `Given an Apple token is granted, when linking Apple, then the repository links with token and nonce`() =
        runTest {
            coEvery { socialSignInProvider.requestToken(method = SignInMethod.APPLE) } returns
                SocialTokenResult.Apple(idToken = ID_TOKEN, rawNonce = NONCE)
            coEvery {
                authenticationRepository.linkWithApple(idToken = ID_TOKEN, rawNonce = NONCE)
            } returns LinkAccountResult.SUCCESS

            val result = useCase(method = SignInMethod.APPLE)

            assertEquals(expected = LinkAccountResult.SUCCESS, actual = result)
            coVerify {
                authenticationRepository.linkWithApple(idToken = ID_TOKEN, rawNonce = NONCE)
            }
        }

    @Test
    fun `Given the picker is cancelled, when linking, then the result is CANCELLED and the repository is not touched`() =
        runTest {
            coEvery { socialSignInProvider.requestToken(method = SignInMethod.GOOGLE) } returns
                SocialTokenResult.Cancelled

            val result = useCase(method = SignInMethod.GOOGLE)

            assertEquals(expected = LinkAccountResult.CANCELLED, actual = result)
            coVerify(exactly = 0) {
                authenticationRepository.linkWithGoogle(idToken = any(), accessToken = any())
            }
        }

    @Test
    fun `Given the provider is not wired up, when linking, then the result is NOT_AVAILABLE`() =
        runTest {
            coEvery { socialSignInProvider.requestToken(method = SignInMethod.GOOGLE) } returns
                SocialTokenResult.NotAvailable

            val result = useCase(method = SignInMethod.GOOGLE)

            assertEquals(expected = LinkAccountResult.NOT_AVAILABLE, actual = result)
        }

    @Test
    fun `When linking by email, then the repository links the email credential directly`() =
        runTest {
            coEvery {
                authenticationRepository.linkWithEmail(email = EMAIL, password = PASSWORD)
            } returns LinkAccountResult.CREDENTIAL_IN_USE

            val result = useCase.withEmail(email = EMAIL, password = PASSWORD)

            assertEquals(expected = LinkAccountResult.CREDENTIAL_IN_USE, actual = result)
            coVerify { authenticationRepository.linkWithEmail(email = EMAIL, password = PASSWORD) }
        }

    private companion object {
        private const val ID_TOKEN = "id-token-123"
        private const val ACCESS_TOKEN = "access-token-789"
        private const val NONCE = "raw-nonce-456"
        private const val EMAIL = "wizard@example.com"
        private const val PASSWORD = "spellbook"
    }
}
