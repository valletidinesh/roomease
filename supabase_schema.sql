-- ============================================================
-- RoomEase — Supabase PostgreSQL Schema
-- Run this in: Supabase Dashboard → SQL Editor → New Query
-- ============================================================

-- Enable UUID generation
create extension if not exists "pgcrypto";

-- ── rooms ────────────────────────────────────────────────────
create table if not exists rooms (
  id               uuid primary key default gen_random_uuid(),
  name             text not null,
  admin_uid        uuid not null,
  invite_code      text not null unique,
  master_order     jsonb not null default '[]',   -- ordered list of user UIDs
  washroom_groups  jsonb not null default '{}',   -- {"1": [uid,uid], "2": [uid,uid]}
  created_at       timestamptz default now()
);

-- ── users ────────────────────────────────────────────────────
create table if not exists users (
  uid                uuid primary key,            -- same as Supabase auth.users.id
  room_id            uuid not null references rooms(id) on delete cascade,
  name               text not null,
  email              text,
  phone              text,
  master_order       int not null default 0,      -- 0-based position in rotation
  status             text not null default 'ACTIVE'   check (status in ('ACTIVE','INACTIVE')),
  presence           text not null default 'PRESENT'  check (presence in ('PRESENT','AWAY')),
  washroom_group     int not null default 1,
  water_fetch_count  int not null default 0,
  last_fetched_at    timestamptz,
  trash_wet_count    int not null default 0,
  trash_dry_count    int not null default 0,
  last_trash_at      timestamptz,
  joined_at          timestamptz default now()
);

create index on users(room_id);

-- ── group_rotation_state ──────────────────────────────────────
create table if not exists group_rotation_state (
  id                   uuid primary key default gen_random_uuid(),
  room_id              uuid not null references rooms(id) on delete cascade,
  group_key            text not null,             -- e.g. "uid_A,uid_B,uid_D"
  sequence             jsonb not null default '[]',
  current_cycle_order  jsonb not null default '[]',
  cycle_index          int not null default 0,
  last_actual_user_id  uuid,
  current_cycle_num    int not null default 1,
  updated_at           timestamptz default now(),
  unique(room_id, group_key)
);

create index on group_rotation_state(room_id, group_key);

-- ── cooking_history ───────────────────────────────────────────
create table if not exists cooking_history (
  id               uuid primary key default gen_random_uuid(),
  room_id          uuid not null references rooms(id) on delete cascade,
  group_key        text not null,
  assigned_user_id uuid not null,
  actual_user_id   uuid not null,
  cooked_at        timestamptz default now(),
  cycle_number     int not null default 1
);

create index on cooking_history(room_id, group_key, cycle_number);

-- ── trash_history ─────────────────────────────────────────────
create table if not exists trash_history (
  id                   uuid primary key default gen_random_uuid(),
  room_id              uuid not null references rooms(id) on delete cascade,
  user_id              uuid not null,
  trash_type           text not null check (trash_type in ('WET','DRY')),
  thrown_at            timestamptz default now(),
  complete_turns_after int not null default 0
);

create index on trash_history(room_id);

-- ── washroom_state ────────────────────────────────────────────
create table if not exists washroom_state (
  id               uuid primary key default gen_random_uuid(),
  room_id          uuid not null references rooms(id) on delete cascade,
  washroom_number  int not null check (washroom_number in (1,2)),
  group_order      jsonb not null default '[]',
  cycle_index      int not null default 0,
  status           text not null default 'ACTIVE' check (status in ('ACTIVE','PENDING_RESUME')),
  unique(room_id, washroom_number)
);

-- ── water_history ─────────────────────────────────────────────
create table if not exists water_history (
  id          uuid primary key default gen_random_uuid(),
  room_id     uuid not null references rooms(id) on delete cascade,
  pair        jsonb not null default '[]',        -- [uid1, uid2]
  fetched_at  timestamptz default now()
);

create index on water_history(room_id);

-- ── purchase_entries ──────────────────────────────────────────
create table if not exists purchase_entries (
  id           uuid primary key default gen_random_uuid(),
  room_id      uuid not null references rooms(id) on delete cascade,
  item         text not null,
  total_qty    int not null,
  total_price  numeric(10,2) not null,
  bought_by    uuid not null,
  bought_at    timestamptz default now(),
  status       text not null default 'OPEN' check (status in ('OPEN','CLOSED')),
  final_split  jsonb not null default '{}'
);

create index on purchase_entries(room_id, status);

-- ── usage_logs ────────────────────────────────────────────────
create table if not exists usage_logs (
  id                 uuid primary key default gen_random_uuid(),
  room_id            uuid not null references rooms(id) on delete cascade,
  purchase_entry_id  uuid not null references purchase_entries(id) on delete cascade,
  user_id            uuid not null,
  qty                int not null check (qty > 0),
  logged_at          timestamptz default now()
);

create index on usage_logs(room_id, purchase_entry_id);

-- ── buy_list ──────────────────────────────────────────────────
create table if not exists buy_list (
  id         uuid primary key default gen_random_uuid(),
  room_id    uuid not null references rooms(id) on delete cascade,
  item_name  text not null,
  added_by   uuid not null,
  added_at   timestamptz default now(),
  status     text not null default 'PENDING' check (status in ('PENDING','BOUGHT')),
  bought_by  uuid,
  bought_at  timestamptz
);

create index on buy_list(room_id, status);

-- ============================================================
-- Row-Level Security (RLS)
-- Ensures users can only read/write data in their own room.
-- ============================================================

alter table rooms             enable row level security;
alter table users             enable row level security;
alter table group_rotation_state enable row level security;
alter table cooking_history   enable row level security;
alter table trash_history     enable row level security;
alter table washroom_state    enable row level security;
alter table water_history     enable row level security;
alter table purchase_entries  enable row level security;
alter table usage_logs        enable row level security;
alter table buy_list          enable row level security;

-- Helper function: is the current auth user a member of a room?
create or replace function is_room_member(p_room_id uuid)
returns boolean language sql security definer as $$
  select exists (
    select 1 from users
    where uid = auth.uid()
      and room_id = p_room_id
  );
$$;

-- Helper: is the current user the admin of a room?
create or replace function is_room_admin(p_room_id uuid)
returns boolean language sql security definer as $$
  select exists (
    select 1 from rooms
    where id = p_room_id
      and admin_uid = auth.uid()
  );
$$;

-- rooms: anyone authenticated can read (needed for join-by-code);
--         members can update; admin deletes
create policy "rooms_select" on rooms for select using (auth.role() = 'authenticated');
create policy "rooms_insert" on rooms for insert with check (auth.role() = 'authenticated');
create policy "rooms_update" on rooms for update using (is_room_admin(id));
create policy "rooms_delete" on rooms for delete using (is_room_admin(id));

-- users
create policy "users_select" on users for select using (is_room_member(room_id));
create policy "users_insert" on users for insert with check (auth.role() = 'authenticated');
create policy "users_update" on users for update using (
  is_room_member(room_id) and (uid = auth.uid() or is_room_admin(room_id))
);

-- All other tables: members can read/write within their room
do $$
declare
  t text;
begin
  foreach t in array array[
    'group_rotation_state','cooking_history','trash_history',
    'washroom_state','water_history','purchase_entries','usage_logs','buy_list'
  ] loop
    execute format('
      create policy "%s_select" on %s for select using (is_room_member(room_id));
      create policy "%s_insert" on %s for insert with check (is_room_member(room_id));
      create policy "%s_update" on %s for update using (is_room_member(room_id));
    ', t, t, t, t, t, t);
  end loop;
end $$;

-- buy_list delete: only the person who added can delete
create policy "buy_list_delete" on buy_list for delete
  using (is_room_member(room_id) and added_by = auth.uid());

-- cooking_history delete: members can delete (for 3-cycle pruning)
create policy "cooking_history_delete" on cooking_history for delete
  using (is_room_member(room_id));

-- ============================================================
-- Realtime — enable publication for all tables
-- ============================================================
alter publication supabase_realtime add table users;
alter publication supabase_realtime add table group_rotation_state;
alter publication supabase_realtime add table cooking_history;
alter publication supabase_realtime add table trash_history;
alter publication supabase_realtime add table washroom_state;
alter publication supabase_realtime add table water_history;
alter publication supabase_realtime add table purchase_entries;
alter publication supabase_realtime add table usage_logs;
alter publication supabase_realtime add table buy_list;
