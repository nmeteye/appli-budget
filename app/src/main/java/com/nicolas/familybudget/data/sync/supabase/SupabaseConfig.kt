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
    const val URL: String = "https://gmhovgndzxqftaxsihju.supabase.co"
    const val ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdtaG92Z25kenhxZnRheHNpaGp1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE2MDMxMzksImV4cCI6MjA5NzE3OTEzOX0.4MOaIW1KOhoXNrOzV6ypEJCYcXWVoNcnfHXn-H0_gr0"

    /** Permet de masquer la section de sync tant que le backend n'est pas configure. */
    val isConfigured: Boolean
        get() = !URL.contains("gmhovgndzxqftaxsihju") && ANON_KEY != "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdtaG92Z25kenhxZnRheHNpaGp1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE2MDMxMzksImV4cCI6MjA5NzE3OTEzOX0.4MOaIW1KOhoXNrOzV6ypEJCYcXWVoNcnfHXn-H0_gr0"
}
