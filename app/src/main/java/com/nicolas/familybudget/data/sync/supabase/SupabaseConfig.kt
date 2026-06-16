package com.nicolas.familybudget.data.sync.supabase

/**
 * Configuration du backend Supabase.
 *
 * Renseigne l'URL et la cle ANON ("publishable") de ton projet :
 *   Supabase > Project Settings > Data API.
 *
 * La cle anon est concue pour etre embarquee dans une app cliente : la securite
 * reelle vient des politiques RLS cote serveur, pas du secret de cette cle.
 *
 * Pour ne pas committer ces valeurs, tu peux a la place les injecter via BuildConfig
 * depuis local.properties / les secrets GitHub Actions (voir README_SYNC.md).
 */
object SupabaseConfig {
    const val URL: String = "https://VOTRE-PROJET.supabase.co"
    const val ANON_KEY: String = "VOTRE_CLE_ANON"

    /** Permet de masquer la section de sync tant que le backend n'est pas configure. */
    val isConfigured: Boolean
        get() = !URL.contains("VOTRE-PROJET") && ANON_KEY != "VOTRE_CLE_ANON"
}
