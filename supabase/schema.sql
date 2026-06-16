-- =====================================================================
--  Family Budget — schema Supabase pour le budget partage multi-utilisateurs
--  A coller dans Supabase > SQL Editor > New query, puis Run.
--
--  Conventions alignees sur le client Android :
--   - cle d'identite inter-appareils = sync_id (TEXT, fournie par le client) ;
--   - references inter-tables par sync_id (jamais par l'id Long local) ;
--   - updated_at = epoch millis (BIGINT), pose par le client (last-write-wins) ;
--   - suppressions logiques via is_deleted (tombstones) ;
--   - le solde des comptes n'est PAS stocke ici (derive cote client).
-- =====================================================================

-- ---------- Budgets ----------
create table if not exists public.budgets (
    id          uuid primary key default gen_random_uuid(),
    name        text not null,
    owner_id    uuid not null references auth.users (id) on delete cascade,
    updated_at  bigint not null default 0,
    is_deleted  boolean not null default false
);

-- ---------- Membres d'un budget ----------
create table if not exists public.budget_members (
    budget_id  uuid not null references public.budgets (id) on delete cascade,
    user_id    uuid not null references auth.users (id) on delete cascade,
    role       text not null default 'editor', -- 'owner' | 'editor'
    primary key (budget_id, user_id)
);

-- ---------- Comptes ----------
create table if not exists public.accounts (
    sync_id      text primary key,
    budget_id    uuid not null references public.budgets (id) on delete cascade,
    name         text not null,
    type         text not null,
    owner_label  text not null default 'Commun',
    color_argb   integer,
    archived     boolean not null default false,
    created_at   bigint not null default 0,
    updated_at   bigint not null default 0,
    is_deleted   boolean not null default false
);
create index if not exists accounts_budget_updated_idx
    on public.accounts (budget_id, updated_at);

-- ---------- Categories ----------
create table if not exists public.categories (
    sync_id              text primary key,
    budget_id            uuid not null references public.budgets (id) on delete cascade,
    name                 text not null,
    kind                 text not null,
    bucket               text not null,
    emoji                text not null default '',
    monthly_budget_cents bigint not null default 0,
    updated_at           bigint not null default 0,
    is_deleted           boolean not null default false
);
create index if not exists categories_budget_updated_idx
    on public.categories (budget_id, updated_at);

-- ---------- Transactions ----------
create table if not exists public.transactions (
    sync_id           text primary key,
    budget_id         uuid not null references public.budgets (id) on delete cascade,
    account_sync_id   text not null,
    category_sync_id  text,
    amount_cents      bigint not null,
    date_epoch_millis bigint not null,
    label             text not null,
    note              text,
    type              text not null,
    transfer_group_id text,
    source            text not null default 'MANUAL',
    created_at        bigint not null default 0,
    updated_at        bigint not null default 0,
    is_deleted        boolean not null default false
);
create index if not exists transactions_budget_updated_idx
    on public.transactions (budget_id, updated_at);

-- =====================================================================
--  Fonctions d'aide (SECURITY DEFINER) : elles s'executent avec les droits
--  du proprietaire et CONTOURNENT la RLS, ce qui casse la recursion de
--  politique sur budget_members (une policy qui interrogerait budget_members).
-- =====================================================================
create or replace function public.is_budget_member(b uuid)
returns boolean language sql security definer set search_path = public as $$
    select exists (
        select 1 from public.budget_members m
        where m.budget_id = b and m.user_id = auth.uid()
    );
$$;

create or replace function public.is_budget_owner(b uuid)
returns boolean language sql security definer set search_path = public as $$
    select exists (
        select 1 from public.budgets bu
        where bu.id = b and bu.owner_id = auth.uid()
    );
$$;

-- ---------- Trigger : l'auteur d'un budget en devient membre 'owner' ----------
create or replace function public.add_owner_membership()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    insert into public.budget_members (budget_id, user_id, role)
    values (new.id, new.owner_id, 'owner')
    on conflict (budget_id, user_id) do nothing;
    return new;
end;
$$;

drop trigger if exists trg_add_owner_membership on public.budgets;
create trigger trg_add_owner_membership
    after insert on public.budgets
    for each row execute function public.add_owner_membership();

-- =====================================================================
--  Row Level Security
-- =====================================================================
alter table public.budgets        enable row level security;
alter table public.budget_members enable row level security;
alter table public.accounts       enable row level security;
alter table public.categories     enable row level security;
alter table public.transactions   enable row level security;

-- ----- budgets -----
drop policy if exists budgets_select on public.budgets;
create policy budgets_select on public.budgets
    for select using (public.is_budget_member(id) or owner_id = auth.uid());

drop policy if exists budgets_insert on public.budgets;
create policy budgets_insert on public.budgets
    for insert with check (owner_id = auth.uid());

drop policy if exists budgets_update on public.budgets;
create policy budgets_update on public.budgets
    for update using (public.is_budget_owner(id)) with check (public.is_budget_owner(id));

drop policy if exists budgets_delete on public.budgets;
create policy budgets_delete on public.budgets
    for delete using (public.is_budget_owner(id));

-- ----- budget_members -----
drop policy if exists members_select on public.budget_members;
create policy members_select on public.budget_members
    for select using (public.is_budget_member(budget_id));

-- L'owner gere les membres ; un utilisateur peut aussi se retirer lui-meme.
drop policy if exists members_insert on public.budget_members;
create policy members_insert on public.budget_members
    for insert with check (public.is_budget_owner(budget_id));

drop policy if exists members_delete on public.budget_members;
create policy members_delete on public.budget_members
    for delete using (public.is_budget_owner(budget_id) or user_id = auth.uid());

-- ----- accounts / categories / transactions : reserve aux membres du budget -----
drop policy if exists accounts_rw on public.accounts;
create policy accounts_rw on public.accounts
    for all using (public.is_budget_member(budget_id))
    with check (public.is_budget_member(budget_id));

drop policy if exists categories_rw on public.categories;
create policy categories_rw on public.categories
    for all using (public.is_budget_member(budget_id))
    with check (public.is_budget_member(budget_id));

drop policy if exists transactions_rw on public.transactions;
create policy transactions_rw on public.transactions
    for all using (public.is_budget_member(budget_id))
    with check (public.is_budget_member(budget_id));

-- =====================================================================
--  Inviter un second utilisateur dans un budget (a faire pour l'instant a la
--  main, en attendant l'UI d'invitation) :
--    1. La personne cree son compte dans l'app (elle apparait dans auth.users).
--    2. Recupere son user_id :  select id, email from auth.users;
--    3. Ajoute-la au budget :
--       insert into public.budget_members (budget_id, user_id, role)
--       values ('<BUDGET_ID>', '<USER_ID>', 'editor');
--    4. Cote app, elle ouvre Reglages > "Mes budgets" et selectionne le budget.
-- =====================================================================
