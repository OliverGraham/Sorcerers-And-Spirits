package com.linklabs.sorcerersandspirits.domain.authentication.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.authentication.SocialTokenResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SignInWithSocialUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk()
    private val socialSignInProvider: SocialSignInProvider = mockk()

    private val useCase =
        SignInWithSocialUseCase(
            authenticationRepository = authenticationRepository,
            socialSignInProvider = socialSignInProvider,
        )

    @Test
    fun `Given a Google token is granted, when signing in, then the repository signs in (not links) with that token`() = runTest {
        coEvery { socialSignInProvider.requestToken(method = SignInMethod.GOOGLE) } returns
            SocialTokenResult.Google(idToken = ID_TOKEN, accessToken = ACCESS_TOKEN)
        coEvery {
            authenticationRepository.signInWithGoogle(
                idToken = ID_TOKEN,
                accessToken = ACCESS_TOKEN,
            )
        } returns LinkAccountResult.SUCCESS

        val result = useCase(method = SignInMethod.GOOGLE)

        assertEquals(expected = LinkAccountResult.SUCCESS, actual = result)
        coVerify {
            authenticationRepository.signInWithGoogle(
                idToken = ID_TOKEN,
                accessToken = ACCESS_TOKEN,
            )
        }
        coVerify(exactly = 0) {
            authenticationRepository.linkWithGoogle(idToken = any(), accessToken = any())
        }
    }

    @Test
    fun `Given the picker is cancelled, when signing in, then the result is CANCELLED and the repository is not touched`() = runTest {
        coEvery { socialSignInProvider.requestToken(method = SignInMethod.GOOGLE) } returns
            SocialTokenResult.Cancelled

        val result = useCase(method = SignInMethod.GOOGLE)

        assertEquals(expected = LinkAccountResult.CANCELLED, actual = result)
        coVerify(exactly = 0) {
            authenticationRepository.signInWithGoogle(idToken = any(), accessToken = any())
        }
    }

    @Test
    fun `When signing in by email, then the repository signs in (recover-or-create) with that email`() = runTest {
        coEvery {
            authenticationRepository.signInWithEmail(email = EMAIL, password = PASSWORD)
        } returns LinkAccountResult.SUCCESS

        val result = useCase.withEmail(email = EMAIL, password = PASSWORD)

        assertEquals(expected = LinkAccountResult.SUCCESS, actual = result)
        coVerify {
            authenticationRepository.signInWithEmail(email = EMAIL, password = PASSWORD)
        }
    }

    private companion object {
        private const val ID_TOKEN = "id-token-123"
        private const val ACCESS_TOKEN = "access-token-789"
        private const val EMAIL = "wizard@example.com"
        private const val PASSWORD = "spellbook"
    }
}
