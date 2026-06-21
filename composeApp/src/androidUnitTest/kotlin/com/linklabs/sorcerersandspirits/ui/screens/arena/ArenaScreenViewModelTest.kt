package com.linklabs.sorcerersandspirits.ui.screens.arena

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ExitArenaUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.TestCharacterClasses
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileStatsUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.isImpersonateRedundant
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ArenaScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val chooseCharacterClassUseCase: ChooseCharacterClassUseCase = mockk()
    private val coordinateArenaMatchUseCase: CoordinateArenaMatchUseCase = mockk()
    private val exitArenaUseCase: ExitArenaUseCase = mockk(relaxed = true)
    private val getCharacterClassesUseCase: GetCharacterClassesUseCase = mockk()
    private val getUserIdUseCase: GetUserIdUseCase = mockk()
    private val performCombatActionUseCase: PerformCombatActionUseCase = mockk()
    private val stayForAnotherMatchUseCase: StayForAnotherMatchUseCase = mockk(relaxed = true)
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase =
        mockk(relaxed = true)
    private val updateProfileStatsUseCase: UpdateProfileStatsUseCase = mockk(relaxed = true)

    private val combatActionArgumentsSlot = slot<CombatActionArguments>()
    private val currentCombatActionSlot = slot<CurrentCombatAction>()

    private val arguments =
        ArenaScreenViewModelArguments(arenaId = "test_arena", expectedNumberOfPlayers = 2)
    private val arenaMatchFlow = MutableStateFlow<ArenaMatch?>(null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getUserIdUseCase() } returns USER_ID
        every { getCharacterClassesUseCase() } returns TestCharacterClasses.all()
        coEvery { coordinateArenaMatchUseCase(any(), any()) } returns arenaMatchFlow
        coEvery {
            performCombatActionUseCase(
                arguments = capture(combatActionArgumentsSlot),
                currentAction = capture(currentCombatActionSlot),
            )
        } returns Unit
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        ArenaScreenViewModel(
            arguments = arguments,
            chooseCharacterClassUseCase = chooseCharacterClassUseCase,
            coordinateArenaMatchUseCase = coordinateArenaMatchUseCase,
            exitArenaUseCase = exitArenaUseCase,
            getCharacterClassesUseCase = getCharacterClassesUseCase,
            getUserIdUseCase = getUserIdUseCase,
            performCombatActionUseCase = performCombatActionUseCase,
            stayForAnotherMatchUseCase = stayForAnotherMatchUseCase,
            setCurrentTravelerAvailableUseCase = setCurrentTravelerAvailableUseCase,
            updateProfileStatsUseCase = updateProfileStatsUseCase,
        )

    /**
     * Seeds [arenaMatchFlow] with a combat match, creates the ViewModel, and advances the
     * dispatcher so the loaded [ArenaState.Combat] is available via [requireCombatState].
     */
    private fun TestScope.combatViewModel(
        nextCombatAction: NextCombatAction,
        playerMap: Map<String, ArenaPlayer.Selected>,
    ): ArenaScreenViewModel {
        arenaMatchFlow.value =
            createCombatMatch(nextCombatAction = nextCombatAction, playerMap = playerMap)
        return createViewModel().also { runCurrent() }
    }

    /** Two-player convenience wrapper that also returns the loaded [ArenaState.Combat]. */
    private fun TestScope.createViewModelWithCombatState(
        nextCombatAction: NextCombatAction,
        userClass: CharacterClass,
        opponentClass: CharacterClass = TestCharacterClasses.mage(),
    ): Pair<ArenaScreenViewModel, ArenaState.Combat> {
        val viewModel =
            combatViewModel(
                nextCombatAction = nextCombatAction,
                playerMap = twoPlayerMap(userClass = userClass, opponentClass = opponentClass),
            )
        return viewModel to viewModel.requireCombatState()
    }

    private fun ArenaScreenViewModel.requireCombatState(): ArenaState.Combat {
        val uiState = uiStateFlow.value
        assertIs<UiState.Loaded<ArenaScreenState>>(uiState)
        return assertIs<ArenaState.Combat>(uiState.data.arenaState)
    }

    private fun ArenaState.Combat.attackControl(): ArenaCombatControl.NormalAttack =
        combatControls.filterIsInstance<ArenaCombatControl.NormalAttack>().single()

    private fun ArenaState.Combat.abilityRollControl(): ArenaCombatControl.AbilityRoll =
        combatControls.filterIsInstance<ArenaCombatControl.AbilityRoll>().single()

    private inline fun <reified T : AbilityActionState> ArenaState.Combat.activatedControlFor():
        ArenaCombatControl.ActivatedAbility =
        combatControls.filterIsInstance<ArenaCombatControl.ActivatedAbility>().single {
            it.action is T
        }

    /** Asserts the user can only make a normal attack on their turn (no activated ability). */
    private fun ArenaState.Combat.assertOnlyAttackOffered() {
        assertTrue(actual = isItThisUsersTurn)
        assertTrue(actual = attackControl().requiresTarget)
        assertTrue(actual = combatControls.none { it is ArenaCombatControl.ActivatedAbility })
    }

    private fun ArenaScreenViewModel.useAbility(
        action: AbilityActionState,
        targetedOpponentUserId: String = "",
        userPlayerId: String = USER_ID,
    ) =
        onUseAbility(
            useAbilityArguments =
                UseAbilityArguments(
                    abilityActionState = action,
                    targetedOpponentUserId = targetedOpponentUserId,
                    userPlayerId = userPlayerId,
                )
        )

    private fun ArenaScreenViewModel.attack(
        combatActionState: CombatActionState,
        targetedOpponentUserId: String,
        userPlayerId: String = USER_ID,
    ) =
        onAttack(
            attackArguments =
                AttackArguments(
                    combatActionState = combatActionState,
                    targetedOpponentUserId = targetedOpponentUserId,
                    userPlayerId = userPlayerId,
                )
        )

    private fun capturedAbilityRoll(): CurrentCombatAction.AbilityRoll =
        assertIs<CurrentCombatAction.AbilityRoll>(value = currentCombatActionSlot.captured)

    private fun capturedNormalAttack(): CurrentCombatAction.NormalAttack =
        assertIs<CurrentCombatAction.NormalAttack>(value = currentCombatActionSlot.captured)

    /**
     * Sets up a defender-side passive ability roll owned by the user, then asserts the offered
     * [ArenaCombatControl.AbilityRoll] matches the expected label and target requirement.
     */
    private fun TestScope.assertPassiveRollControl(
        purpose: AbilityAction,
        userClass: CharacterClass,
        opponentClass: CharacterClass = TestCharacterClasses.mage(),
        attackerUserId: String = OPPONENT_ID,
        defenderUserId: String = USER_ID,
        expectedLabel: String,
        expectedRequiresTarget: Boolean,
    ) {
        val combatState =
            combatViewModel(
                    nextCombatAction =
                        abilityRollAction(
                            purpose = purpose,
                            attackerUserId = attackerUserId,
                            defenderUserId = defenderUserId,
                            activeUserIds = setOf(USER_ID),
                        ),
                    playerMap = twoPlayerMap(userClass = userClass, opponentClass = opponentClass),
                )
                .requireCombatState()

        val control = combatState.abilityRollControl()
        assertEquals(expected = expectedLabel, actual = control.label)
        assertEquals(expected = expectedRequiresTarget, actual = control.requiresTarget)
    }

    // ---------- Domain data -> UI state: which controls are available ----------

    @Test
    fun `Given an idle turn for the user's Gambler, then the UI offers an attack and an enabled All In control`() =
        runTest {
            val (_, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(USER_ID)),
                    userClass = TestCharacterClasses.gambler(),
                )

            assertTrue(actual = combatState.isItThisUsersTurn)

            val attackControl =
                combatState.combatControls
                    .filterIsInstance<ArenaCombatControl.NormalAttack>()
                    .single()
            assertTrue(actual = attackControl.requiresTarget)

            val allInControl =
                combatState.combatControls
                    .filterIsInstance<ArenaCombatControl.ActivatedAbility>()
                    .single()
            assertIs<AbilityActionState.Activated.AllIn>(value = allInControl.action)
            assertEquals(expected = "All In", actual = allInControl.label)
            assertTrue(actual = allInControl.enabled)
            assertFalse(actual = allInControl.requiresTarget)
        }

    @Test
    fun `Given the user's Gambler already used All In this turn, then the All In control is disabled but still visible`() =
        runTest {
            val gambler =
                TestCharacterClasses.gambler()
                    .copy(
                        offensiveAbility =
                            OffensiveAbility.AllIn(hasAnotherAttack = true, usedThisTurn = true)
                    )

            val (_, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(USER_ID)),
                    userClass = gambler,
                )

            val allInControl =
                combatState.combatControls
                    .filterIsInstance<ArenaCombatControl.ActivatedAbility>()
                    .single()
            assertEquals(expected = "All In", actual = allInControl.label)
            assertFalse(actual = allInControl.enabled)
        }

    @Test
    fun `Given an idle turn for the user's Duelist, then the UI offers an attack and a Prepare Attack control that requires a target`() =
        runTest {
            val (_, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(USER_ID)),
                    userClass = TestCharacterClasses.duelist(),
                )

            val attackControl =
                combatState.combatControls
                    .filterIsInstance<ArenaCombatControl.NormalAttack>()
                    .single()
            assertTrue(actual = attackControl.requiresTarget)

            val prepareAttackControl =
                combatState.combatControls
                    .filterIsInstance<ArenaCombatControl.ActivatedAbility>()
                    .single()
            assertIs<AbilityActionState.Activated.PrepareAttack>(
                value = prepareAttackControl.action
            )
            assertEquals(expected = "Prepare Attack", actual = prepareAttackControl.label)
            assertTrue(actual = prepareAttackControl.requiresTarget)
            assertTrue(actual = prepareAttackControl.enabled)
        }

    @Test
    fun `Given an idle turn for the user's Necromancer, then the Summon Minion control does not require a target`() =
        runTest {
            val (_, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(USER_ID)),
                    userClass = TestCharacterClasses.necromancer(),
                )

            val summonMinionControl =
                combatState.combatControls
                    .filterIsInstance<ArenaCombatControl.ActivatedAbility>()
                    .single()
            assertIs<AbilityActionState.Activated.SummonMinion>(value = summonMinionControl.action)
            assertEquals(expected = "Summon Minion", actual = summonMinionControl.label)
            assertFalse(actual = summonMinionControl.requiresTarget)
        }

    @Test
    fun `Given an idle turn for an opponent, then the user has no combat controls`() = runTest {
        val (_, combatState) =
            createViewModelWithCombatState(
                nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(OPPONENT_ID)),
                userClass = TestCharacterClasses.gambler(),
            )

        assertFalse(actual = combatState.isItThisUsersTurn)
        assertTrue(actual = combatState.combatControls.isEmpty())
    }

    // ---------- Pending mid-attack rolls -> available controls ----------

    @Test
    fun `Given the user's pending health roll, then only an attack control without a target requirement is offered`() =
        runTest {
            val (_, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction =
                        NextCombatAction.NormalAttack(
                            roll = NormalAttackType.Health,
                            attackerUserId = USER_ID,
                            defenderUserId = OPPONENT_ID,
                            originalTurnUserId = USER_ID,
                            activeUserIds = setOf(USER_ID),
                        ),
                    userClass = TestCharacterClasses.duelist(),
                )

            val attackControl = combatState.combatControls.single()
            assertIs<ArenaCombatControl.NormalAttack>(value = attackControl)
            assertFalse(actual = attackControl.requiresTarget)
        }

    @Test
    fun `Given a Teleport roll-off where the user's die is unrolled, then only the Teleport control is offered`() =
        runTest {
            val (_, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = createTeleportRollOff(attackerRoll = 4, defenderRoll = null),
                    userClass = TestCharacterClasses.mage(),
                    opponentClass = TestCharacterClasses.duelist(),
                )

            val teleportControl = combatState.combatControls.single()
            assertIs<ArenaCombatControl.AbilityRoll>(value = teleportControl)
            assertEquals(expected = "Teleport", actual = teleportControl.label)
            assertFalse(actual = teleportControl.requiresTarget)
        }

    @Test
    fun `Given an ability roll that belongs to an opponent, then the user has no combat controls`() =
        runTest {
            val (_, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction =
                        NextCombatAction.AbilityRoll(
                            purpose = AbilityAction.Passive.Riposte(attackerRollTotal = 7),
                            attackerUserId = USER_ID,
                            defenderUserId = OPPONENT_ID,
                            originalTurnUserId = USER_ID,
                            activeUserIds = setOf(OPPONENT_ID),
                        ),
                    userClass = TestCharacterClasses.gambler(),
                    opponentClass = TestCharacterClasses.duelist(),
                )

            assertTrue(actual = combatState.combatControls.isEmpty())
        }

    // ---------- UI action -> Domain conversion ----------

    @Test
    fun `When the user attacks from idle, then an ArmorClass roll is sent to the Domain with the user as attacker`() =
        runTest {
            val (viewModel, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(USER_ID)),
                    userClass = TestCharacterClasses.duelist(),
                )

            viewModel.onAttack(
                attackArguments =
                    AttackArguments(
                        combatActionState = combatState.combatActionState,
                        targetedOpponentUserId = OPPONENT_ID,
                        userPlayerId = USER_ID,
                    )
            )
            runCurrent()

            val currentAction = currentCombatActionSlot.captured
            assertIs<CurrentCombatAction.NormalAttack>(value = currentAction)
            assertEquals(expected = NormalAttackType.ArmorClass, actual = currentAction.roll)
            assertEquals(expected = USER_ID, actual = currentAction.attackerUserId)
            assertEquals(expected = OPPONENT_ID, actual = currentAction.defenderUserId)
            assertEquals(expected = USER_ID, actual = currentAction.originalTurnUserId)

            val combatArguments = combatActionArgumentsSlot.captured
            assertEquals(expected = arguments.arenaId, actual = combatArguments.arenaId)
            assertEquals(expected = 1, actual = combatArguments.roundNumber)
            assertEquals(
                expected = CharacterClassType.Duelist,
                actual = combatArguments.playerMap.getValue(key = USER_ID).characterClass.type,
            )
        }

    @Test
    fun `When the user activates the offered All In control, then an All In ability action without a target is sent to the Domain`() =
        runTest {
            val (viewModel, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(USER_ID)),
                    userClass = TestCharacterClasses.gambler(),
                )

            val allInControl =
                combatState.combatControls
                    .filterIsInstance<ArenaCombatControl.ActivatedAbility>()
                    .single()

            viewModel.onUseAbility(
                useAbilityArguments =
                    UseAbilityArguments(
                        abilityActionState = allInControl.action,
                        targetedOpponentUserId = "",
                        userPlayerId = USER_ID,
                    )
            )
            runCurrent()

            val abilityRoll =
                assertIs<CurrentCombatAction.AbilityRoll>(value = currentCombatActionSlot.captured)
            val allIn = assertIs<AbilityAction.Activated.AllIn>(value = abilityRoll.purpose)
            assertFalse(actual = allIn.allInAbility.usedThisTurn)

            assertEquals(expected = USER_ID, actual = abilityRoll.attackerUserId)
            assertEquals(expected = USER_ID, actual = abilityRoll.defenderUserId)
            assertEquals(expected = USER_ID, actual = abilityRoll.originalTurnUserId)
        }

    @Test
    fun `When the user activates the offered Prepare Attack control with a target, then a Prepare Attack ability action targets that opponent`() =
        runTest {
            val (viewModel, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(USER_ID)),
                    userClass = TestCharacterClasses.duelist(),
                )

            val prepareAttackControl =
                combatState.combatControls
                    .filterIsInstance<ArenaCombatControl.ActivatedAbility>()
                    .single()

            viewModel.onUseAbility(
                useAbilityArguments =
                    UseAbilityArguments(
                        abilityActionState = prepareAttackControl.action,
                        targetedOpponentUserId = OPPONENT_ID,
                        userPlayerId = USER_ID,
                    )
            )
            runCurrent()

            val abilityRoll =
                assertIs<CurrentCombatAction.AbilityRoll>(value = currentCombatActionSlot.captured)
            assertIs<AbilityAction.Activated.PrepareAttack>(value = abilityRoll.purpose)

            assertEquals(expected = USER_ID, actual = abilityRoll.attackerUserId)
            assertEquals(expected = OPPONENT_ID, actual = abilityRoll.defenderUserId)
        }

    @Test
    fun `When the user makes the offered Teleport roll, then the roll-off combatants are resolved from the Teleport state`() =
        runTest {
            val (viewModel, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = createTeleportRollOff(attackerRoll = 4, defenderRoll = null),
                    userClass = TestCharacterClasses.mage(),
                    opponentClass = TestCharacterClasses.duelist(),
                )

            val teleportControl =
                combatState.combatControls
                    .filterIsInstance<ArenaCombatControl.AbilityRoll>()
                    .single()

            viewModel.onUseAbility(
                useAbilityArguments =
                    UseAbilityArguments(
                        abilityActionState = teleportControl.action,
                        targetedOpponentUserId = "",
                        userPlayerId = USER_ID,
                    )
            )
            runCurrent()

            val abilityRoll =
                assertIs<CurrentCombatAction.AbilityRoll>(value = currentCombatActionSlot.captured)
            val teleport = assertIs<AbilityAction.Passive.Teleport>(value = abilityRoll.purpose)
            assertEquals(expected = OPPONENT_ID, actual = teleport.attackerRoll.userId)
            assertEquals(expected = 4, actual = teleport.attackerRoll.roll)
            assertEquals(expected = USER_ID, actual = teleport.defenderRoll.userId)
            assertNull(actual = teleport.defenderRoll.roll)

            assertEquals(expected = OPPONENT_ID, actual = abilityRoll.attackerUserId)
            assertEquals(expected = USER_ID, actual = abilityRoll.defenderUserId)
        }

    // ---------- Deferred Finished transition (SS-098) ----------

    @Test
    fun `Given a match finishes with closing messages, then the Finished transition is deferred until the messages are handled`() =
        runTest {
            val playerMap = twoPlayerMap(userClass = TestCharacterClasses.rogue())
            val viewModel =
                combatViewModel(nextCombatAction = idleTurn(OPPONENT_ID), playerMap = playerMap)

            arenaMatchFlow.value =
                ArenaMatch.Finished(
                    messages =
                        listOf(
                            IncomingArenaMessage(
                                id = "message-1",
                                message = ArenaMessage.NewRound(roundNumber = 1),
                            )
                        ),
                    numberOfNewMessages = 1,
                    playerMap = playerMap,
                )
            runCurrent()

            val deferredState = (viewModel.uiStateFlow.value as UiState.Loaded).data
            assertIs<ArenaState.Combat>(deferredState.arenaState)
            assertNotNull(deferredState.pendingFinishedState)

            viewModel.onIncomingMessageHandled()
            runCurrent()

            val finishedState = (viewModel.uiStateFlow.value as UiState.Loaded).data
            assertIs<ArenaState.Finished>(finishedState.arenaState)
            assertNull(finishedState.pendingFinishedState)
        }

    @Test
    fun `Given the finished countdown expires and the user has not readied, then the user is auto-readied`() =
        runTest {
            val playerMap = twoPlayerMap(userClass = TestCharacterClasses.rogue())
            val viewModel =
                combatViewModel(nextCombatAction = idleTurn(OPPONENT_ID), playerMap = playerMap)

            // The opponent readying starts the shared countdown while the user has not readied.
            val countdownPlayerMap =
                playerMap +
                    (OPPONENT_ID to
                        playerMap.getValue(OPPONENT_ID).copy(isReadyForNextMatch = true))
            arenaMatchFlow.value =
                ArenaMatch.Finished(
                    messages = emptyList(),
                    numberOfNewMessages = 0,
                    playerMap = countdownPlayerMap,
                )
            runCurrent()

            advanceUntilIdle()

            coVerify { stayForAnotherMatchUseCase(arenaId = arguments.arenaId) }
        }

    // ---------- Forfeit dialog ----------

    @Test
    fun `onTryLeaveArena shows forfeit dialog when state is WaitingForAllPlayers`() = runTest {
        arenaMatchFlow.value =
            ArenaMatch.WaitingForAllPlayers(
                messages = emptyList(),
                numberOfNewMessages = 0,
                playerMap =
                    mapOf(USER_ID to ArenaPlayer.Unselected(userId = USER_ID, displayName = "User")),
            )
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()

            // Loaded (initial)
            val initialState = awaitItem()
            assertTrue(initialState is UiState.Loaded)
            assertFalse(initialState.data.showForfeitDialog)

            viewModel.onTryLeaveArena()

            // Loaded (with dialog)
            val updatedState = awaitItem()
            assertTrue(updatedState is UiState.Loaded)
            assertTrue(updatedState.data.showForfeitDialog, "Dialog should be shown")
        }
    }

    @Test
    fun `onConfirmLeaveArena calls use case and hides dialog`() = runTest {
        arenaMatchFlow.value =
            ArenaMatch.WaitingForAllPlayers(
                messages = emptyList(),
                numberOfNewMessages = 0,
                playerMap =
                    mapOf(USER_ID to ArenaPlayer.Unselected(userId = USER_ID, displayName = "User")),
            )
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()
            // Loaded
            awaitItem()

            viewModel.onTryLeaveArena()
            assertTrue((awaitItem() as UiState.Loaded).data.showForfeitDialog)

            viewModel.onConfirmLeaveArena()
            assertFalse((awaitItem() as UiState.Loaded).data.showForfeitDialog)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify { exitArenaUseCase(arenaId = arguments.arenaId) }
    }

    @Test
    fun `onDismissForfeitDialog hides dialog`() = runTest {
        arenaMatchFlow.value =
            ArenaMatch.WaitingForAllPlayers(
                messages = emptyList(),
                numberOfNewMessages = 0,
                playerMap =
                    mapOf(USER_ID to ArenaPlayer.Unselected(userId = USER_ID, displayName = "User")),
            )
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()
            // Loaded
            awaitItem()

            viewModel.onTryLeaveArena()
            assertTrue((awaitItem() as UiState.Loaded).data.showForfeitDialog)

            viewModel.onDismissForfeitDialog()
            assertFalse((awaitItem() as UiState.Loaded).data.showForfeitDialog)
        }
    }

    @Test
    fun `onStayForAnotherMatch calls use case and updates state`() = runTest {
        arenaMatchFlow.value =
            ArenaMatch.Finished(
                messages = emptyList(),
                numberOfNewMessages = 0,
                playerMap =
                    mapOf(USER_ID to ArenaPlayer.Unselected(userId = USER_ID, displayName = "User")),
            )
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()
            // Loaded
            val initialState = awaitItem() as UiState.Loaded
            assertFalse(initialState.data.hasReadiedForNextMatch)

            viewModel.onStayForAnotherMatch()

            // Loaded (updated)
            val updatedState = awaitItem() as UiState.Loaded
            assertTrue(updatedState.data.hasReadiedForNextMatch)
        }

        coVerify { stayForAnotherMatchUseCase(arenaId = arguments.arenaId) }
    }

    @Test
    fun `participantClassNames maintains class names even after players leave`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()

            // 1. Initial Match with two players who have chosen classes
            arenaMatchFlow.value =
                ArenaMatch.WaitingForAllPlayers(
                    messages = emptyList(),
                    numberOfNewMessages = 0,
                    playerMap =
                        mapOf(
                            USER_ID to
                                selectedPlayer(USER_ID, "Alice", TestCharacterClasses.rogue()),
                            OPPONENT_ID to
                                selectedPlayer(OPPONENT_ID, "Bob", TestCharacterClasses.mage()),
                        ),
                )

            val state1 = (awaitItem() as UiState.Loaded).data
            assertEquals(CharacterClassType.Rogue.name, state1.participantClassNames[USER_ID])
            assertEquals(CharacterClassType.Mage.name, state1.participantClassNames[OPPONENT_ID])

            // 2. Opponent leaves
            arenaMatchFlow.value =
                ArenaMatch.WaitingForAllPlayers(
                    messages = emptyList(),
                    numberOfNewMessages = 0,
                    playerMap =
                        mapOf(
                            USER_ID to
                                selectedPlayer(USER_ID, "Alice", TestCharacterClasses.rogue())
                        ),
                )

            val state2 = (awaitItem() as UiState.Loaded).data
            assertEquals(CharacterClassType.Rogue.name, state2.participantClassNames[USER_ID])
            // Bob's class should still be here so their earlier messages still resolve!
            assertEquals(CharacterClassType.Mage.name, state2.participantClassNames[OPPONENT_ID])
        }
    }

    @Test
    fun `Given an opponent leaves, then an OpponentLeft event names them`() = runTest {
        arenaMatchFlow.value =
            ArenaMatch.WaitingForAllPlayers(
                messages = emptyList(),
                numberOfNewMessages = 0,
                playerMap =
                    mapOf(
                        USER_ID to selectedPlayer(USER_ID, "Alice", TestCharacterClasses.rogue()),
                        OPPONENT_ID to
                            selectedPlayer(OPPONENT_ID, "Bob", TestCharacterClasses.mage()),
                    ),
            )
        val viewModel = createViewModel()

        viewModel.eventChannelFlow.test {
            runCurrent()

            arenaMatchFlow.value =
                ArenaMatch.WaitingForAllPlayers(
                    messages = emptyList(),
                    numberOfNewMessages = 0,
                    playerMap =
                        mapOf(
                            USER_ID to
                                selectedPlayer(USER_ID, "Alice", TestCharacterClasses.rogue())
                        ),
                )
            runCurrent()

            val event = awaitItem()
            assertIs<ArenaEvent.OpponentLeft>(event)
            assertEquals("Bob", event.displayName)
        }
    }

    // ---------- Idle-turn controls per class (no activated ability) ----------

    @Test
    fun `Given an idle turn for the user's Rogue, then only an attack control is offered`() =
        runTest {
            combatViewModel(
                    nextCombatAction = idleTurn(USER_ID),
                    playerMap = twoPlayerMap(userClass = TestCharacterClasses.rogue()),
                )
                .requireCombatState()
                .assertOnlyAttackOffered()
        }

    @Test
    fun `Given an idle turn for the user's Bard, then only an attack control is offered`() =
        runTest {
            combatViewModel(
                    nextCombatAction = idleTurn(USER_ID),
                    playerMap = twoPlayerMap(userClass = TestCharacterClasses.bard()),
                )
                .requireCombatState()
                .assertOnlyAttackOffered()
        }

    @Test
    fun `Given an idle turn for the user's Mage, then only an attack control is offered`() =
        runTest {
            combatViewModel(
                    nextCombatAction = idleTurn(USER_ID),
                    playerMap = twoPlayerMap(userClass = TestCharacterClasses.mage()),
                )
                .requireCombatState()
                .assertOnlyAttackOffered()
        }

    @Test
    fun `Given an idle turn for the user's Paladin, then only an attack control is offered`() =
        runTest {
            combatViewModel(
                    nextCombatAction = idleTurn(USER_ID),
                    playerMap = twoPlayerMap(userClass = TestCharacterClasses.paladin()),
                )
                .requireCombatState()
                .assertOnlyAttackOffered()
        }

    @Test
    fun `Given a two-player attack-only idle turn, then the lone opponent is auto-targeted`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap = twoPlayerMap(userClass = TestCharacterClasses.rogue()),
                    )
                    .requireCombatState()

            combatState.assertOnlyAttackOffered()
            assertEquals(expected = OPPONENT_ID, actual = combatState.autoTargetedOpponentUserId)
        }

    @Test
    fun `Given an attack-only idle turn with two opponents, then no opponent is auto-targeted`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap =
                            threePlayerMap(
                                userClass = TestCharacterClasses.rogue(),
                                firstOpponentClass = TestCharacterClasses.mage(),
                                secondOpponentClass = TestCharacterClasses.paladin(),
                            ),
                    )
                    .requireCombatState()

            assertNull(actual = combatState.autoTargetedOpponentUserId)
        }

    @Test
    fun `Given a two-player idle turn with an activated ability available, then no opponent is auto-targeted`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap = twoPlayerMap(userClass = TestCharacterClasses.duelist()),
                    )
                    .requireCombatState()

            assertNull(actual = combatState.autoTargetedOpponentUserId)
        }

    @Test
    fun `Given a two-player attack-only opponent turn, then no opponent is auto-targeted`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(OPPONENT_ID),
                        playerMap = twoPlayerMap(userClass = TestCharacterClasses.rogue()),
                    )
                    .requireCombatState()

            assertNull(actual = combatState.autoTargetedOpponentUserId)
        }

    @Test
    fun `Given a two-player idle turn whose only enabled action is attack beside a disabled All-In, then the lone opponent is auto-targeted`() =
        runTest {
            val gambler =
                TestCharacterClasses.gambler()
                    .copy(
                        offensiveAbility =
                            OffensiveAbility.AllIn(hasAnotherAttack = true, usedThisTurn = true)
                    )

            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap = twoPlayerMap(userClass = gambler),
                    )
                    .requireCombatState()

            val allInControl = combatState.activatedControlFor<AbilityActionState.Activated.AllIn>()
            assertFalse(actual = allInControl.enabled)
            assertEquals(expected = OPPONENT_ID, actual = combatState.autoTargetedOpponentUserId)
        }

    @Test
    fun `Given an idle turn for the user's Illusionist, then an Impersonate control that requires a target is offered`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap = twoPlayerMap(userClass = TestCharacterClasses.illusionist()),
                    )
                    .requireCombatState()

            val impersonateControl =
                combatState.activatedControlFor<AbilityActionState.Activated.Impersonate>()
            assertEquals(expected = "Impersonate", actual = impersonateControl.label)
            assertTrue(actual = impersonateControl.requiresTarget)
            // A fresh Illusionist has copied nothing, so Impersonate is the only activated control
            assertEquals(
                expected = 1,
                actual =
                    combatState.combatControls.count { it is ArenaCombatControl.ActivatedAbility },
            )
        }

    @Test
    fun `Given the user's Illusionist has impersonated All In, then both Impersonate and the copied All In controls are offered`() =
        runTest {
            val illusionist =
                TestCharacterClasses.illusionist()
                    .copy(
                        offensiveAbility =
                            OffensiveAbility.Impersonate(
                                impersonatedAbility = OffensiveAbility.AllIn()
                            )
                    )

            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap = twoPlayerMap(userClass = illusionist),
                    )
                    .requireCombatState()

            val activatedControls =
                combatState.combatControls.filterIsInstance<ArenaCombatControl.ActivatedAbility>()
            assertEquals(expected = 2, actual = activatedControls.size)
            assertTrue(
                actual =
                    activatedControls.any { it.action is AbilityActionState.Activated.Impersonate }
            )
            assertTrue(
                actual = activatedControls.any { it.action is AbilityActionState.Activated.AllIn }
            )
        }

    @Test
    fun `Given the user's Illusionist has impersonated Prepare Attack, then the copied control requires a target`() =
        runTest {
            val illusionist =
                TestCharacterClasses.illusionist()
                    .copy(
                        offensiveAbility =
                            OffensiveAbility.Impersonate(
                                impersonatedAbility = OffensiveAbility.PrepareAttack
                            )
                    )

            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap = twoPlayerMap(userClass = illusionist),
                    )
                    .requireCombatState()

            val prepareAttackControl =
                combatState.activatedControlFor<AbilityActionState.Activated.PrepareAttack>()
            assertEquals(expected = "Prepare Attack", actual = prepareAttackControl.label)
            assertTrue(actual = prepareAttackControl.requiresTarget)
        }

    @Test
    fun `Given the Illusionist already impersonated a class, then Impersonate is redundant only for an opponent of that same class`() =
        runTest {
            val illusionist =
                TestCharacterClasses.illusionist()
                    .copy(
                        offensiveAbility =
                            OffensiveAbility.Impersonate(
                                impersonatedAbility = OffensiveAbility.PrepareAttack
                            )
                    )

            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap =
                            threePlayerMap(
                                userClass = illusionist,
                                firstOpponentClass = TestCharacterClasses.duelist(),
                                secondOpponentClass = TestCharacterClasses.mage(),
                            ),
                    )
                    .requireCombatState()

            val impersonateControl =
                combatState.activatedControlFor<AbilityActionState.Activated.Impersonate>()
            assertTrue(
                actual =
                    combatState.isImpersonateRedundant(
                        control = impersonateControl,
                        targetedOpponentUserId = OPPONENT_ID,
                    )
            )
            assertFalse(
                actual =
                    combatState.isImpersonateRedundant(
                        control = impersonateControl,
                        targetedOpponentUserId = SECOND_OPPONENT_ID,
                    )
            )
        }

    @Test
    fun `Given the Illusionist has copied nothing, then Impersonate is never redundant`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap = twoPlayerMap(userClass = TestCharacterClasses.illusionist()),
                    )
                    .requireCombatState()

            val impersonateControl =
                combatState.activatedControlFor<AbilityActionState.Activated.Impersonate>()
            assertFalse(
                actual =
                    combatState.isImpersonateRedundant(
                        control = impersonateControl,
                        targetedOpponentUserId = OPPONENT_ID,
                    )
            )
        }

    // ---------- Controls while an attack is in progress ----------

    @Test
    fun `Given a normal attack in progress where the user is not the attacker, then the user has no combat controls`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction =
                            normalAttackAction(
                                roll = NormalAttackType.ArmorClass,
                                attackerUserId = OPPONENT_ID,
                                defenderUserId = USER_ID,
                            ),
                        playerMap = twoPlayerMap(userClass = TestCharacterClasses.rogue()),
                    )
                    .requireCombatState()

            assertFalse(actual = combatState.isItThisUsersTurn)
            assertTrue(actual = combatState.combatControls.isEmpty())
        }

    @Test
    fun `Given a Teleport roll-off where the user has already rolled, then no Teleport control is offered`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction =
                            createTeleportRollOff(attackerRoll = null, defenderRoll = 5),
                        playerMap =
                            twoPlayerMap(
                                userClass = TestCharacterClasses.mage(),
                                opponentClass = TestCharacterClasses.duelist(),
                            ),
                    )
                    .requireCombatState()

            assertTrue(actual = combatState.combatControls.isEmpty())
        }

    // ---------- Passive ability roll controls owned by the user ----------

    @Test
    fun `Given a Riposte roll that belongs to the user's Duelist, then a Riposte control is offered without a target`() =
        runTest {
            assertPassiveRollControl(
                purpose = AbilityAction.Passive.Riposte(attackerRollTotal = 7),
                userClass = TestCharacterClasses.duelist(),
                opponentClass = TestCharacterClasses.rogue(),
                expectedLabel = "Riposte",
                expectedRequiresTarget = false,
            )
        }

    @Test
    fun `Given a Lucky Coin flip roll that belongs to the user's Gambler, then a Lucky Coin control is offered without a target`() =
        runTest {
            assertPassiveRollControl(
                purpose = AbilityAction.Passive.LuckyCoin.Flip(attackerRollTotal = 8),
                userClass = TestCharacterClasses.gambler(),
                expectedLabel = "Lucky Coin",
                expectedRequiresTarget = false,
            )
        }

    @Test
    fun `Given a Lucky Coin high-roller roll that belongs to the user's Gambler, then a Lucky Coin control is offered without a target`() =
        runTest {
            assertPassiveRollControl(
                purpose = AbilityAction.Passive.LuckyCoin.HighRoller(attackerRollTotal = 9),
                userClass = TestCharacterClasses.gambler(),
                expectedLabel = "Lucky Coin",
                expectedRequiresTarget = false,
            )
        }

    @Test
    fun `Given a Persuade strum-lute roll that belongs to the user's Bard, then a Persuade control is offered without a target`() =
        runTest {
            assertPassiveRollControl(
                purpose = AbilityAction.Passive.Persuade.StrumLute,
                userClass = TestCharacterClasses.bard(),
                expectedLabel = "Persuade",
                expectedRequiresTarget = false,
            )
        }

    @Test
    fun `Given a Persuade charm that belongs to the user's Bard, then a Persuade control that requires a target is offered`() =
        runTest {
            assertPassiveRollControl(
                purpose =
                    AbilityAction.Passive.Persuade.Charm(targetableUserIds = setOf(OPPONENT_ID)),
                userClass = TestCharacterClasses.bard(),
                expectedLabel = "Persuade",
                expectedRequiresTarget = true,
            )
        }

    @Test
    fun `Given a Mirror Image roll that belongs to the user's Illusionist, then reroll-either-die and keep controls are offered`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction =
                            abilityRollAction(
                                purpose =
                                    AbilityAction.Passive.MirrorImage(
                                        firstDie =
                                            AbilityAction.Passive.MirrorImage.MirrorImageDie(
                                                originalRoll = 3
                                            ),
                                        secondDie =
                                            AbilityAction.Passive.MirrorImage.MirrorImageDie(
                                                originalRoll = 4
                                            ),
                                        attackType = NormalAttackType.ArmorClass,
                                        mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
                                    ),
                                attackerUserId = OPPONENT_ID,
                                defenderUserId = USER_ID,
                                activeUserIds = setOf(USER_ID),
                            ),
                        playerMap = twoPlayerMap(userClass = TestCharacterClasses.illusionist()),
                    )
                    .requireCombatState()

            val controls =
                combatState.combatControls.filterIsInstance<ArenaCombatControl.AbilityRoll>()
            assertEquals(
                expected = listOf("Reroll the 3", "Reroll the 4", "Keep roll"),
                actual = controls.map { it.label },
            )
            assertTrue(actual = controls.none { it.requiresTarget })

            val selections =
                controls.map {
                    (it.action as AbilityActionState.Passive.MirrorImage).rerollSelection
                }
            assertEquals(
                expected =
                    listOf(
                        AbilityAction.Passive.MirrorImage.RerollSelection.First,
                        AbilityAction.Passive.MirrorImage.RerollSelection.Second,
                        AbilityAction.Passive.MirrorImage.RerollSelection.None,
                    ),
                actual = selections,
            )
        }

    @Test
    fun `Given a Holy Shield prayer roll that belongs to the user's Paladin, then a Holy Shield control is offered without a target`() =
        runTest {
            assertPassiveRollControl(
                purpose = AbilityAction.Passive.HolyShield.Prayer,
                userClass = TestCharacterClasses.paladin(),
                opponentClass = TestCharacterClasses.rogue(),
                expectedLabel = "Holy Shield",
                expectedRequiresTarget = false,
            )
        }

    @Test
    fun `Given a Holy Shield wrath roll that belongs to the user's Paladin, then a Holy Shield control is offered without a target`() =
        runTest {
            assertPassiveRollControl(
                purpose = AbilityAction.Passive.HolyShield.Wrath,
                userClass = TestCharacterClasses.paladin(),
                opponentClass = TestCharacterClasses.rogue(),
                expectedLabel = "Holy Shield",
                expectedRequiresTarget = false,
            )
        }

    @Test
    fun `Given an Encore roll that belongs to the user's attacking Bard, then an Encore control is offered without a target`() =
        runTest {
            assertPassiveRollControl(
                purpose = AbilityAction.Passive.Encore,
                userClass = TestCharacterClasses.bard(),
                attackerUserId = USER_ID,
                defenderUserId = OPPONENT_ID,
                expectedLabel = "Encore",
                expectedRequiresTarget = false,
            )
        }

    // ---------- UI action -> Domain conversion (abilities) ----------

    @Test
    fun `When the user activates Summon Minion, then a Summon Minion ability action targets the user themselves`() =
        runTest {
            val viewModel =
                combatViewModel(
                    nextCombatAction = idleTurn(USER_ID),
                    playerMap = twoPlayerMap(userClass = TestCharacterClasses.necromancer()),
                )
            val control =
                viewModel
                    .requireCombatState()
                    .activatedControlFor<AbilityActionState.Activated.SummonMinion>()

            viewModel.useAbility(action = control.action)
            runCurrent()

            val abilityRoll = capturedAbilityRoll()
            assertIs<AbilityAction.Activated.SummonMinion>(value = abilityRoll.purpose)
            assertEquals(expected = USER_ID, actual = abilityRoll.attackerUserId)
            assertEquals(expected = USER_ID, actual = abilityRoll.defenderUserId)
            assertEquals(expected = USER_ID, actual = abilityRoll.originalTurnUserId)
        }

    @Test
    fun `When the user activates Impersonate with a target, then an Impersonate action copies that opponent's offensive ability`() =
        runTest {
            val viewModel =
                combatViewModel(
                    nextCombatAction = idleTurn(USER_ID),
                    playerMap =
                        twoPlayerMap(
                            userClass = TestCharacterClasses.illusionist(),
                            opponentClass = TestCharacterClasses.mage(),
                        ),
                )
            val control =
                viewModel
                    .requireCombatState()
                    .activatedControlFor<AbilityActionState.Activated.Impersonate>()

            viewModel.useAbility(action = control.action, targetedOpponentUserId = OPPONENT_ID)
            runCurrent()

            val abilityRoll = capturedAbilityRoll()
            val impersonate =
                assertIs<AbilityAction.Activated.Impersonate>(value = abilityRoll.purpose)
            assertEquals(expected = USER_ID, actual = abilityRoll.attackerUserId)
            assertEquals(expected = OPPONENT_ID, actual = abilityRoll.defenderUserId)
            // The targeted Mage's offensive ability is copied, not the Illusionist's own (None).
            assertEquals(
                expected = OffensiveAbility.SpellPotency,
                actual = impersonate.impersonateAbility.impersonatedAbility,
            )
        }

    @Test
    fun `When the user activates a copied All In via Impersonate, then an All In ability action is sent to the Domain`() =
        runTest {
            val illusionist =
                TestCharacterClasses.illusionist()
                    .copy(
                        offensiveAbility =
                            OffensiveAbility.Impersonate(
                                impersonatedAbility = OffensiveAbility.AllIn()
                            )
                    )
            val viewModel =
                combatViewModel(
                    nextCombatAction = idleTurn(USER_ID),
                    playerMap = twoPlayerMap(userClass = illusionist),
                )
            val allInControl =
                viewModel
                    .requireCombatState()
                    .activatedControlFor<AbilityActionState.Activated.AllIn>()

            viewModel.useAbility(action = allInControl.action)
            runCurrent()

            val abilityRoll = capturedAbilityRoll()
            assertIs<AbilityAction.Activated.AllIn>(value = abilityRoll.purpose)
            assertEquals(expected = USER_ID, actual = abilityRoll.attackerUserId)
        }

    @Test
    fun `When the user continues a normal attack with a pending health roll, then a health roll is sent with the same combatants`() =
        runTest {
            val viewModel =
                combatViewModel(
                    nextCombatAction =
                        normalAttackAction(
                            roll = NormalAttackType.Health,
                            attackerUserId = USER_ID,
                            defenderUserId = OPPONENT_ID,
                            activeUserIds = setOf(USER_ID),
                        ),
                    playerMap = twoPlayerMap(userClass = TestCharacterClasses.duelist()),
                )
            val combatState = viewModel.requireCombatState()

            viewModel.attack(
                combatActionState = combatState.combatActionState,
                targetedOpponentUserId = OPPONENT_ID,
            )
            runCurrent()

            val normalAttack = capturedNormalAttack()
            assertEquals(expected = NormalAttackType.Health, actual = normalAttack.roll)
            assertEquals(expected = USER_ID, actual = normalAttack.attackerUserId)
            assertEquals(expected = OPPONENT_ID, actual = normalAttack.defenderUserId)
        }

    @Test
    fun `When the user's Duelist responds with Riposte, then the ability roll keeps the original attacker and defender`() =
        runTest {
            val viewModel =
                combatViewModel(
                    nextCombatAction =
                        abilityRollAction(
                            purpose = AbilityAction.Passive.Riposte(attackerRollTotal = 7),
                            attackerUserId = OPPONENT_ID,
                            defenderUserId = USER_ID,
                            activeUserIds = setOf(USER_ID),
                        ),
                    playerMap =
                        twoPlayerMap(
                            userClass = TestCharacterClasses.duelist(),
                            opponentClass = TestCharacterClasses.rogue(),
                        ),
                )
            val control = viewModel.requireCombatState().abilityRollControl()

            viewModel.useAbility(action = control.action)
            runCurrent()

            val abilityRoll = capturedAbilityRoll()
            assertIs<AbilityAction.Passive.Riposte>(value = abilityRoll.purpose)
            assertEquals(expected = OPPONENT_ID, actual = abilityRoll.attackerUserId)
            assertEquals(expected = USER_ID, actual = abilityRoll.defenderUserId)
            assertEquals(expected = OPPONENT_ID, actual = abilityRoll.originalTurnUserId)
        }

    @Test
    fun `When the user's Paladin responds with Holy Shield, then the ability roll keeps the original combatants`() =
        runTest {
            val viewModel =
                combatViewModel(
                    nextCombatAction =
                        abilityRollAction(
                            purpose = AbilityAction.Passive.HolyShield.Prayer,
                            attackerUserId = OPPONENT_ID,
                            defenderUserId = USER_ID,
                            activeUserIds = setOf(USER_ID),
                        ),
                    playerMap =
                        twoPlayerMap(
                            userClass = TestCharacterClasses.paladin(),
                            opponentClass = TestCharacterClasses.rogue(),
                        ),
                )
            val control = viewModel.requireCombatState().abilityRollControl()

            viewModel.useAbility(action = control.action)
            runCurrent()

            val abilityRoll = capturedAbilityRoll()
            assertIs<AbilityAction.Passive.HolyShield.Prayer>(value = abilityRoll.purpose)
            assertEquals(expected = OPPONENT_ID, actual = abilityRoll.attackerUserId)
            assertEquals(expected = USER_ID, actual = abilityRoll.defenderUserId)
        }

    // ---------- Three-player scenarios ----------

    @Test
    fun `Given a three-player idle turn for the user's Duelist, then attack and Prepare Attack controls are offered`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(USER_ID),
                        playerMap =
                            threePlayerMap(
                                userClass = TestCharacterClasses.duelist(),
                                firstOpponentClass = TestCharacterClasses.rogue(),
                                secondOpponentClass = TestCharacterClasses.mage(),
                            ),
                    )
                    .requireCombatState()

            assertTrue(actual = combatState.isItThisUsersTurn)
            assertTrue(actual = combatState.attackControl().requiresTarget)
            val prepareAttack =
                combatState.activatedControlFor<AbilityActionState.Activated.PrepareAttack>()
            assertTrue(actual = prepareAttack.requiresTarget)
        }

    @Test
    fun `Given a three-player game where it is an opponent's turn, then the user has no combat controls`() =
        runTest {
            val combatState =
                combatViewModel(
                        nextCombatAction = idleTurn(OPPONENT_ID),
                        playerMap =
                            threePlayerMap(
                                userClass = TestCharacterClasses.rogue(),
                                firstOpponentClass = TestCharacterClasses.duelist(),
                                secondOpponentClass = TestCharacterClasses.mage(),
                            ),
                    )
                    .requireCombatState()

            assertFalse(actual = combatState.isItThisUsersTurn)
            assertTrue(actual = combatState.combatControls.isEmpty())
        }

    @Test
    fun `When the user attacks a chosen opponent in a three-player game, then that opponent is the defender`() =
        runTest {
            val viewModel =
                combatViewModel(
                    nextCombatAction = idleTurn(USER_ID),
                    playerMap =
                        threePlayerMap(
                            userClass = TestCharacterClasses.rogue(),
                            firstOpponentClass = TestCharacterClasses.duelist(),
                            secondOpponentClass = TestCharacterClasses.mage(),
                        ),
                )
            val combatState = viewModel.requireCombatState()

            viewModel.attack(
                combatActionState = combatState.combatActionState,
                targetedOpponentUserId = SECOND_OPPONENT_ID,
            )
            runCurrent()

            val normalAttack = capturedNormalAttack()
            assertEquals(expected = NormalAttackType.ArmorClass, actual = normalAttack.roll)
            assertEquals(expected = USER_ID, actual = normalAttack.attackerUserId)
            assertEquals(expected = SECOND_OPPONENT_ID, actual = normalAttack.defenderUserId)
        }

    @Test
    fun `When the user's Bard charms a chosen target in a three-player game, then the charm target is set on the ability action`() =
        runTest {
            val viewModel =
                combatViewModel(
                    nextCombatAction =
                        abilityRollAction(
                            purpose =
                                AbilityAction.Passive.Persuade.Charm(
                                    targetableUserIds = setOf(OPPONENT_ID, SECOND_OPPONENT_ID)
                                ),
                            attackerUserId = OPPONENT_ID,
                            defenderUserId = USER_ID,
                            activeUserIds = setOf(USER_ID),
                        ),
                    playerMap =
                        threePlayerMap(
                            userClass = TestCharacterClasses.bard(),
                            firstOpponentClass = TestCharacterClasses.rogue(),
                            secondOpponentClass = TestCharacterClasses.mage(),
                        ),
                )
            val control = viewModel.requireCombatState().abilityRollControl()

            viewModel.useAbility(
                action = control.action,
                targetedOpponentUserId = SECOND_OPPONENT_ID,
            )
            runCurrent()

            val abilityRoll = capturedAbilityRoll()
            val charm = assertIs<AbilityAction.Passive.Persuade.Charm>(value = abilityRoll.purpose)
            assertEquals(expected = SECOND_OPPONENT_ID, actual = charm.selectedCharmTargetUserId)
            assertEquals(
                expected = setOf(OPPONENT_ID, SECOND_OPPONENT_ID),
                actual = charm.targetableUserIds,
            )
        }

    @Test
    fun `When the user's Bard charms an untargetable player, then no charm target is set`() =
        runTest {
            val viewModel =
                combatViewModel(
                    nextCombatAction =
                        abilityRollAction(
                            purpose =
                                AbilityAction.Passive.Persuade.Charm(
                                    targetableUserIds = setOf(SECOND_OPPONENT_ID)
                                ),
                            attackerUserId = OPPONENT_ID,
                            defenderUserId = USER_ID,
                            activeUserIds = setOf(USER_ID),
                        ),
                    playerMap =
                        threePlayerMap(
                            userClass = TestCharacterClasses.bard(),
                            firstOpponentClass = TestCharacterClasses.rogue(),
                            secondOpponentClass = TestCharacterClasses.mage(),
                        ),
                )
            val control = viewModel.requireCombatState().abilityRollControl()

            // The Bard (USER_ID) is not among the charmable targets, so selecting itself is
            // rejected.
            viewModel.useAbility(action = control.action, targetedOpponentUserId = USER_ID)
            runCurrent()

            val abilityRoll = capturedAbilityRoll()
            val charm = assertIs<AbilityAction.Passive.Persuade.Charm>(value = abilityRoll.purpose)
            assertNull(actual = charm.selectedCharmTargetUserId)
        }

    companion object {
        private const val USER_ID = "user_123"
        private const val OPPONENT_ID = "opponent_456"
        private const val SECOND_OPPONENT_ID = "opponent_789"

        private fun selectedPlayer(
            userId: String,
            displayName: String,
            characterClass: CharacterClass,
        ) =
            ArenaPlayer.Selected(
                userId = userId,
                displayName = displayName,
                characterClass = characterClass,
            )

        private fun twoPlayerMap(
            userClass: CharacterClass,
            opponentClass: CharacterClass = TestCharacterClasses.mage(),
        ) =
            mapOf(
                USER_ID to selectedPlayer(USER_ID, "User", userClass),
                OPPONENT_ID to selectedPlayer(OPPONENT_ID, "Opponent", opponentClass),
            )

        private fun threePlayerMap(
            userClass: CharacterClass,
            firstOpponentClass: CharacterClass,
            secondOpponentClass: CharacterClass,
        ) =
            mapOf(
                USER_ID to selectedPlayer(USER_ID, "User", userClass),
                OPPONENT_ID to selectedPlayer(OPPONENT_ID, "Opponent", firstOpponentClass),
                SECOND_OPPONENT_ID to
                    selectedPlayer(SECOND_OPPONENT_ID, "Second Opponent", secondOpponentClass),
            )

        private fun createCombatMatch(
            nextCombatAction: NextCombatAction,
            playerMap: Map<String, ArenaPlayer.Selected>,
            roundNumber: Int = 1,
        ) =
            ArenaMatch.Combat(
                messages = emptyList(),
                numberOfNewMessages = 0,
                playerMap = playerMap,
                nextCombatAction = nextCombatAction,
                roundNumber = roundNumber,
                isNewRound = false,
                doublesBonus = null,
            )

        private fun idleTurn(vararg activeUserIds: String) =
            NextCombatAction.Idle(activeUserIds = activeUserIds.toSet())

        private fun normalAttackAction(
            roll: NormalAttackType,
            attackerUserId: String,
            defenderUserId: String,
            originalTurnUserId: String = attackerUserId,
            activeUserIds: Set<String> = setOf(attackerUserId),
        ) =
            NextCombatAction.NormalAttack(
                roll = roll,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                originalTurnUserId = originalTurnUserId,
                activeUserIds = activeUserIds,
            )

        private fun abilityRollAction(
            purpose: AbilityAction,
            attackerUserId: String,
            defenderUserId: String,
            originalTurnUserId: String = attackerUserId,
            activeUserIds: Set<String>,
        ) =
            NextCombatAction.AbilityRoll(
                purpose = purpose,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                originalTurnUserId = originalTurnUserId,
                activeUserIds = activeUserIds,
            )

        /** A Teleport roll-off where the opponent attacks and the user is the defending Mage. */
        private fun createTeleportRollOff(attackerRoll: Int?, defenderRoll: Int?) =
            abilityRollAction(
                purpose =
                    AbilityAction.Passive.Teleport(
                        attackerRoll =
                            AbilityAction.Passive.Teleport.TeleportDie(
                                userId = OPPONENT_ID,
                                roll = attackerRoll,
                            ),
                        defenderRoll =
                            AbilityAction.Passive.Teleport.TeleportDie(
                                userId = USER_ID,
                                roll = defenderRoll,
                            ),
                    ),
                attackerUserId = OPPONENT_ID,
                defenderUserId = USER_ID,
                activeUserIds = setOf(USER_ID),
            )
    }
}
