package com.linklabs.sorcerersandspirits.domain.arena.usecase

// TODO: Figure out a way to mock the dice roll - do not supply a lambda to the constructor of the
//  use case. Dice rolling will be much more complicated than that in this game. Instead, think
//  making an interface that will have at least two dice roll functions - one for two dice and one
//  for one die. Then the test can implement the interface and mock the results, and the actual
//  use case can keep things real

class PerformCombatActionUseCaseTest
