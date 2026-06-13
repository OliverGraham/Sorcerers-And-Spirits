package com.linklabs.sorcerersandspirits.ui.screens.arena

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ExitArenaUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.RemoveArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.StayForAnotherMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
    private val getUserIdUseCase: GetUserIdUseCase = mockk()
    private val performCombatActionUseCase: PerformCombatActionUseCase = mockk()
    private val removeArenaMatchUseCase: RemoveArenaMatchUseCase = mockk(relaxed = true)
    private val stayForAnotherMatchUseCase: StayForAnotherMatchUseCase = mockk(relaxed = true)
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase =
        mockk(relaxed = true)

    private val combatActionArgumentsSlot = slot<CombatActionArguments>()
    private val currentCombatActionSlot = slot<CurrentCombatAction>()

    private val arguments =
        ArenaScreenViewModelArguments(arenaId = "test_arena", expectedNumberOfPlayers = 2)
    private val arenaMatchFlow = MutableStateFlow<ArenaMatch?>(null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getUserIdUseCase() } returns USER_ID
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
            getUserIdUseCase = getUserIdUseCase,
            performCombatActionUseCase = performCombatActionUseCase,
            removeArenaMatchUseCase = removeArenaMatchUseCase,
            stayForAnotherMatchUseCase = stayForAnotherMatchUseCase,
            setCurrentTravelerAvailableUseCase = setCurrentTravelerAvailableUseCase,
        )

    private fun TestScope.createViewModelWithCombatState(
        nextCombatAction: NextCombatAction,
        userClass: CharacterClass,
        opponentClass: CharacterClass = CharacterClass.getMage(),
    ): Pair<ArenaScreenViewModel, ArenaState.Combat> {
        arenaMatchFlow.value =
            createCombatMatch(
                nextCombatAction = nextCombatAction,
                userClass = userClass,
                opponentClass = opponentClass,
            )

        val viewModel = createViewModel()
        runCurrent()

        return viewModel to viewModel.requireCombatState()
    }

    private fun ArenaScreenViewModel.requireCombatState(): ArenaState.Combat {
        val uiState = uiStateFlow.value
        assertIs<UiState.Loaded<ArenaScreenState>>(uiState)
        return assertIs<ArenaState.Combat>(uiState.data.arenaState)
    }

    // ---------- Domain data -> UI state: which controls are available ----------

    @Test
    fun `Given an idle turn for the user's Gambler, then the UI offers an attack and an enabled All In control`() =
        runTest {
            val (_, combatState) =
                createViewModelWithCombatState(
                    nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(USER_ID)),
                    userClass = CharacterClass.getGambler(),
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
                CharacterClass.getGambler()
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
                    userClass = CharacterClass.getDuelist(),
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
                    userClass = CharacterClass.getNecromancer(),
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
                userClass = CharacterClass.getGambler(),
            )

        assertFalse(actual = combatState.isItThisUsersTurn)
        assertTrue(actual = combatState.combatControls.isEmpty())
    }

    // TODO: All fucked

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
                    userClass = CharacterClass.getDuelist(),
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
                    userClass = CharacterClass.getMage(),
                    opponentClass = CharacterClass.getDuelist(),
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
                    userClass = CharacterClass.getGambler(),
                    opponentClass = CharacterClass.getDuelist(),
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
                    userClass = CharacterClass.getDuelist(),
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
                    userClass = CharacterClass.getGambler(),
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
                    userClass = CharacterClass.getDuelist(),
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
                    userClass = CharacterClass.getMage(),
                    opponentClass = CharacterClass.getDuelist(),
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
            assertFalse(initialState.data.hasMadeKnockoutDecision)

            viewModel.onStayForAnotherMatch()

            // Loaded (updated)
            val updatedState = awaitItem() as UiState.Loaded
            assertTrue(updatedState.data.hasMadeKnockoutDecision)
        }

        coVerify { stayForAnotherMatchUseCase(arenaId = arguments.arenaId) }
    }

    @Test
    fun `participantNames maintains names even after players leave`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()

            // 1. Initial Match with two players
            arenaMatchFlow.value =
                ArenaMatch.WaitingForAllPlayers(
                    messages = emptyList(),
                    numberOfNewMessages = 0,
                    playerMap =
                        mapOf(
                            USER_ID to ArenaPlayer.Unselected(USER_ID, "Alice"),
                            OPPONENT_ID to ArenaPlayer.Unselected(OPPONENT_ID, "Bob"),
                        ),
                )

            val state1 = (awaitItem() as UiState.Loaded).data
            assertEquals("Alice", state1.participantNames[USER_ID])
            assertEquals("Bob", state1.participantNames[OPPONENT_ID])

            // 2. Opponent leaves
            arenaMatchFlow.value =
                ArenaMatch.WaitingForAllPlayers(
                    messages = emptyList(),
                    numberOfNewMessages = 0,
                    playerMap = mapOf(USER_ID to ArenaPlayer.Unselected(USER_ID, "Alice")),
                )

            val state2 = (awaitItem() as UiState.Loaded).data
            assertEquals("Alice", state2.participantNames[USER_ID])
            // Bob should still be here!
            assertEquals("Bob", state2.participantNames[OPPONENT_ID])
        }
    }

    companion object {
        private const val USER_ID = "user_123"
        private const val OPPONENT_ID = "opponent_456"

        private fun createCombatMatch(
            nextCombatAction: NextCombatAction,
            userClass: CharacterClass,
            opponentClass: CharacterClass,
            roundNumber: Int = 1,
        ) =
            ArenaMatch.Combat(
                messages = emptyList(),
                numberOfNewMessages = 0,
                playerMap =
                    mapOf(
                        USER_ID to
                            ArenaPlayer.Selected(
                                userId = USER_ID,
                                displayName = "User",
                                characterClass = userClass,
                            ),
                        OPPONENT_ID to
                            ArenaPlayer.Selected(
                                userId = OPPONENT_ID,
                                displayName = "Opponent",
                                characterClass = opponentClass,
                            ),
                    ),
                nextCombatAction = nextCombatAction,
                roundNumber = roundNumber,
                isNewRound = false,
                doublesBonus = null,
            )

        /** A Teleport roll-off where the opponent attacks and the user is the defending Mage. */
        private fun createTeleportRollOff(attackerRoll: Int?, defenderRoll: Int?) =
            NextCombatAction.AbilityRoll(
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
                originalTurnUserId = OPPONENT_ID,
                activeUserIds = setOf(USER_ID),
            )
    }
}
