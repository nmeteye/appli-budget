# Budget Familial

Application Android **hors-ligne** de gestion de budget familial : plusieurs comptes,
conseils issus de la **finance comportementale**, et un **planificateur de placement**
pédagogique adapté au contexte français. Toutes les données restent sur l'appareil
(Room + DataStore). Aucune donnée n'est envoyée sur un serveur.

> ⚠️ Le module Placement est **pédagogique**. Ce n'est pas un conseil en investissement
> personnalisé ni réglementé. Les rendements ne sont pas garantis. Vérifie les taux et
> plafonds en vigueur et consulte un conseiller agréé pour toute décision engageante.

## Build (workflow GitHub Actions)

Le projet est conçu pour ton flux habituel : *push → build via Actions*.

- `.github/workflows/android.yml` installe le JDK 17 et Gradle 8.11.1, génère le wrapper,
  lance les tests unitaires puis `assembleDebug`, et publie l'APK comme artefact.
- Pas besoin de committer `gradle-wrapper.jar` : l'action `gradle/actions/setup-gradle`
  fournit Gradle, et `gradle wrapper` régénère le wrapper au premier run.

En local (optionnel) : ouvre le dossier dans Android Studio, ou `gradle wrapper` puis
`./gradlew assembleDebug`.

## Stack épinglée

Combinaison fin-2024 éprouvée, choisie pour compiler de façon reproductible :

| Élément        | Version       |
|----------------|---------------|
| Gradle         | 8.11.1        |
| AGP            | 8.7.3         |
| Kotlin         | 2.0.21        |
| KSP            | 2.0.21-1.0.28 |
| Compose BOM    | 2024.12.01    |
| Hilt           | 2.52          |
| Room           | 2.6.1         |
| compile/target | SDK 35        |
| minSdk         | 26            |

Tout est centralisé dans `gradle/libs.versions.toml`. Pour monter en version (Kotlin 2.3,
AGP 9.x, Compose BOM 2025.12.00…), modifie uniquement ce fichier. Note : AGP 9.x introduit
le « Kotlin intégré » et change le DSL — prévois un passage par l'Upgrade Assistant.

## Architecture (MVVM + Hilt)

```
core/        Money (centimes), TimeRange
data/local/  Entities, DAOs, AppDatabase (Room), Converters, SettingsRepository (DataStore)
data/repository/  Account / Transaction / Category / Goal / Family
data/csv/    CsvImporter (import de relevé collé)
data/sync/   BankSyncProvider (interface) + Mock + squelette Powens (DSP2)
domain/      BudgetSummaryUseCase
domain/advice/    BudgetAdviceEngine (conseils comportementaux, chacun porte son `principle`)
domain/invest/    InvestmentPlannerEngine (cascade FR), CompoundInterestSimulator, FrenchProducts
ui/          theme, navigation, dashboard, accounts, transactions, advice, invest, settings
di/          AppModule (Hilt)
```

- **Argent** : stocké en **centimes (Long)** partout, formaté en € (locale FR).
- **Conseils** : moteur de règles transparent (50/30/20, épargne de précaution en mois,
  automatisation « payez-vous d'abord », dette, anticipation des études). Chaque conseil
  expose le mécanisme comportemental qui le motive, donc auditable.
- **Placement** : cascade *dette coûteuse → épargne de précaution (Livret A/LDDS) →
  placement du surplus selon l'horizon dérivé des objectifs* (court → livrets ;
  moyen → assurance-vie ; long → PEA/AV pondéré par l'appétit au risque ; retraite → PER).
  Vérifie les plafonds (Livret A, LDDS, PEA).

## Brancher une vraie synchro bancaire (DSP2 / Open Banking)

Par défaut, `AppModule` fournit `MockBankSyncProvider` (données fictives, hors-ligne).
Une synchro réelle passe par un **agrégateur** (Powens/Budget Insight, Bridge, Tink,
GoCardless…). On **ne peut pas** embarquer de credentials dans l'app : l'enregistrement
DSP2 (statut TPP/agent) et les clés client te sont propres.

Pour l'activer :

1. Implémente `BankSyncProvider` (voir le squelette `PowensBankSyncProvider`) avec tes
   appels REST (Retrofit/Ktor) : `GET /users/me/accounts`, `GET /users/me/transactions`.
2. Gère l'auth (webview de connexion bancaire de l'agrégateur → access token).
3. Remplace le provider fourni dans `AppModule.provideBankSyncProvider()`.
4. Ajoute un mapping `externalId → compte local` durable (ici la démo crée des comptes).

## Pistes d'itération

- Sélecteur de date (DatePicker) pour les opérations et import de fichier via SAF.
- Graphiques de tendance (dépenses par catégorie, patrimoine dans le temps).
- Catégorisation automatique des libellés importés (règles ou modèle local).
- Budgets d'enveloppe par catégorie avec alertes de dépassement.
- Sauvegarde/restauration chiffrée locale.
