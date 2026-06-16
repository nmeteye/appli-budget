# Synchronisation multi-utilisateurs (budget partagé)

Cette fonctionnalité permet à plusieurs personnes de partager un même budget,
chacune sur son téléphone, avec une synchronisation **hors-ligne d'abord** et
**différée** : chaque appareil garde sa base Room locale comme source de vérité,
pousse ses modifications quand il a du réseau, puis tire celles des autres.

L'architecture est **additive** : les clés primaires locales (`id: Long`) sont
conservées ; une colonne `syncId` (UUID) sert d'identité inter-appareils. Aucune
collision d'identifiants entre appareils, et l'app locale fonctionne à l'identique
si la sync n'est pas configurée.

## 1. Créer le projet Supabase

1. Crée un compte sur https://supabase.com puis un nouveau projet.
2. Choisis une **région européenne** (Francfort, Paris, Irlande…) : ce sont des
   données financières, autant rester dans le périmètre RGPD.
3. Dans **SQL Editor**, colle le contenu de `supabase/schema.sql` et exécute-le.

## 2. Brancher l'app sur le projet

Récupère l'URL et la clé **anon / publishable** dans
**Project Settings → Data API**, puis renseigne-les dans :

`app/src/main/java/com/nicolas/familybudget/data/sync/supabase/SupabaseConfig.kt`

```kotlin
const val URL: String = "https://xxxxxxxx.supabase.co"
const val ANON_KEY: String = "eyJhbGciOi..."
```

La clé anon est faite pour être embarquée dans un client : la sécurité réelle
vient des politiques **RLS** définies dans le script SQL, pas du secret de la clé.

> Variante plus propre (optionnelle) : mettre ces deux valeurs dans
> `local.properties` (non commité) et les exposer via `buildConfigField`, en
> alimentant les mêmes clés par des **secrets GitHub Actions** pour la CI.

## 3. Utiliser

Dans l'app : **Réglages → Budget partagé**.

1. Crée un compte (e-mail + mot de passe) et connecte-toi.
2. **Crée un budget** : toutes tes données locales y sont rattachées et poussées.
3. Sur le 2ᵉ téléphone, l'autre personne crée **son** compte et se connecte.
4. Ajoute-la au budget (voir l'encart en bas de `schema.sql` — l'UI d'invitation
   n'est pas encore faite, ça se fait pour l'instant par une requête SQL).
5. Elle ouvre **Réglages → Mes budgets** et sélectionne le budget partagé.

La synchro tourne ensuite périodiquement (~6 h, WorkManager, réseau requis) et via
le bouton **Synchroniser**.

## 4. Points de vigilance / à savoir

- **Versions du toolchain.** Le catalogue épingle `supabase = "3.0.2"` et
  `ktor = "3.0.1"` dans `gradle/libs.versions.toml`. C'est la dernière release de
  supabase-kt compilée avec **Kotlin 2.0.21** (la version du projet) : les versions
  ≥ 3.0.3 passent en Kotlin 2.1+/2.2/2.3 et imposeraient de monter Kotlin, KSP et le
  plugin Compose. Si tu fais ce saut un jour, tu pourras prendre une supabase-kt
  plus récente (jusqu'à 3.5.0 pour Kotlin 2.3.0).
- **Soldes.** Les soldes de comptes ne sont **jamais** synchronisés : ils sont
  recalculés à partir des transactions après chaque pull. C'est volontaire (deux
  appareils qui s'échangeraient des agrégats divergeraient irréversiblement).
- **Conflits.** Résolution *last-write-wins* par ligne sur `updatedAt`. Suffisant
  pour un budget familial ; pas un CRDT.
- **Périmètre v1.** Sont synchronisés : comptes, catégories, transactions. Les
  objectifs (`goals`) et membres du foyer (`family_members`) ne le sont pas encore
  — ils suivront le même patron si besoin.
- **Compilation.** Ces fichiers n'ont pas pu être compilés dans l'environnement de
  rédaction : prévois 1 à 2 itérations via GitHub Actions pour lever d'éventuelles
  erreurs d'API (surtout autour de supabase-kt et du `@HiltWorker`).
