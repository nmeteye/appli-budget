package com.nicolas.familybudget.data.sync.supabase

import com.nicolas.familybudget.BuildConfig

/**
 * Configuration du backend Supabase, injectee a la compilation via BuildConfig
 * (voir app/build.gradle.kts -> secret()). Les valeurs proviennent, par ordre :
 *   1. variables d'environnement  -> secrets GitHub Actions en CI ;
 *   2. local.properties           -> build local (fichier NON versionne) ;
 *   3. propriete Gradle -P         -> ponctuel.
 *
 * On ne committe donc jamais l'URL ni la cle dans le code source.
 *
 * Renseigne, pour un build local, dans local.properties (a la racine du projet) :
 *   SUPABASE_URL=https://xxxx.supabase.co
 *   SUPABASE_ANON_KEY=eyJhbGciOi...
 *
 * Et dans le depot GitHub : Settings > Secrets and variables > Actions, ajoute
 * les secrets SUPABASE_URL et SUPABASE_ANON_KEY (le workflow les passe au build).
 *
 * La cle anon ("publishable") est faite pour etre embarquee dans un client :
 * la securite reelle vient des politiques RLS cote serveur.
 */
object SupabaseConfig {
    val URL: String = BuildConfig.SUPABASE_URL
    val ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    /** Masque la section de sync tant que le backend n'est pas renseigne. */
    val isConfigured: Boolean
        get() = URL.isNotBlank() && ANON_KEY.isNotBlank()
}
