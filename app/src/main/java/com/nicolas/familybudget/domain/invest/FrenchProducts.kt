package com.nicolas.familybudget.domain.invest

/**
 * Reference des produits d'epargne francais.
 *
 * IMPORTANT : les taux ci-dessous sont des HYPOTHESES INDICATIVES et PARAMETRABLES,
 * pas des valeurs officielles en temps reel. Les taux reglementes (Livret A, LDDS, LEP)
 * sont revus periodiquement par les pouvoirs publics ; les rendements des UC, ETF et
 * fonds euros ne sont pas garantis et peuvent etre negatifs. A verifier avant toute
 * decision. Les plafonds sont ceux en vigueur de longue date mais peuvent evoluer.
 */
object FrenchProducts {

    // Plafonds de versement (en centimes).
    const val LIVRET_A_CAP_CENTS = 22_950_00L
    const val LDDS_CAP_CENTS = 12_000_00L
    const val LEP_CAP_CENTS = 10_000_00L
    const val PEA_CAP_CENTS = 150_000_00L

    // Taux annuels indicatifs (defaut modifiable par l'utilisateur dans le simulateur).
    const val DEFAULT_LIVRET_A_RATE = 0.017     // ~1,7 % indicatif
    const val DEFAULT_LDDS_RATE = 0.017
    const val DEFAULT_LEP_RATE = 0.031
    const val DEFAULT_FONDS_EUROS_RATE = 0.025  // assurance-vie, fonds en euros
    const val DEFAULT_EQUITY_RATE = 0.05        // ETF actions long terme, hypothese prudente nette
    const val DEFAULT_INFLATION = 0.02

    const val DISCLAIMER =
        "Informations a but pedagogique. Ceci n'est pas un conseil en investissement " +
            "personnalise ni reglemente. Les rendements ne sont pas garantis et les " +
            "placements en unites de compte ou actions comportent un risque de perte en " +
            "capital. Verifie les taux et plafonds en vigueur, et consulte un conseiller " +
            "agree (CGP) pour une decision engageante."
}
