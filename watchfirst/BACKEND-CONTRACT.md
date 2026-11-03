# Contrato do backend (Supabase)

O app herdado do Nuvio conversa com um projeto Supabase. O esquema SQL não está neste repositório, então cada item abaixo precisa existir no seu projeto com o mesmo nome e um retorno compatível com o que o código decodifica.

A lista foi extraída do código-fonte. Os parâmetros são heurísticos (campos `p_*` achados na função que faz a chamada); confirme no arquivo indicado antes de implementar.

## Configuração no build

Chaves lidas de `local.properties` ou do ambiente: `NUVIO_SUPABASE_URL`, `NUVIO_SUPABASE_ANON_KEY` e, opcionalmente, `NUVIO_SUPABASE_FALLBACK_URL`. Veja `local.properties.example`.

## Autenticação

Supabase Auth com e-mail e senha. O login de TV usa as funções `start_device_login_session` e `poll_tv_login_session` e a edge function `tv-logins-exchange`.

## Funções RPC (46)

### Sincronização de dados (biblioteca, progresso, perfis, addons, coleções, configurações)

| Função | Parâmetros (heurístico) | Chamada em |
|---|---|---|
| `sync_delete_library_items` | `p_profile_id`, `p_keys` | `commonMain/features/library/sync/SupabaseLibrarySyncAdapter.kt:93` |
| `sync_delete_profile_data` | `p_profile_id` | `commonMain/features/profiles/ProfileRepository.kt:310` |
| `sync_delete_watch_progress` | `p_profile_id`, `p_keys` | `commonMain/features/watching/sync/SupabaseProgressSyncAdapter.kt:125` |
| `sync_delete_watched_items` | `p_profile_id`, `p_keys` | `commonMain/features/watching/sync/SupabaseWatchedSyncAdapter.kt:127` |
| `sync_get_library_delta_cursor` | `p_profile_id` | `commonMain/features/library/sync/SupabaseLibrarySyncAdapter.kt:43` |
| `sync_get_watch_progress_delta_cursor` | `p_profile_id` | `commonMain/features/watching/sync/SupabaseProgressSyncAdapter.kt:27` |
| `sync_get_watched_items_delta_cursor` | `p_profile_id` | `commonMain/features/watching/sync/SupabaseWatchedSyncAdapter.kt:27` |
| `sync_pull_collections` | `p_profile_id` | `commonMain/features/collection/CollectionSyncService.kt:53` |
| `sync_pull_home_catalog_settings` | `p_profile_id`, `p_platform` | `commonMain/features/home/HomeCatalogSettingsSyncService.kt:202` |
| `sync_pull_library` | `p_profile_id`, `p_limit`, `p_offset` | `commonMain/features/library/sync/SupabaseLibrarySyncAdapter.kt:33` |
| `sync_pull_library_delta` | `p_profile_id`, `p_since_event_id`, `p_limit` | `commonMain/features/library/sync/SupabaseLibrarySyncAdapter.kt:58` |
| `sync_pull_media_servers` | `p_profile_id` | `commonMain/core/sync/MediaServerSync.kt:105` |
| `sync_pull_profile_locks` | não identificados | `commonMain/features/profiles/ProfileRepository.kt:401` |
| `sync_pull_profile_settings_blob` | `p_profile_id`, `p_platform` | `commonMain/core/sync/ProfileSettingsSync.kt:125` |
| `sync_pull_profiles` | não identificados | `commonMain/features/profiles/ProfileRepository.kt:132` |
| `sync_pull_provider_credentials` | `p_profile_id` | `commonMain/core/sync/ProviderCredentialSync.kt:136` |
| `sync_pull_watch_progress` | `p_profile_id`, `p_since_last_watched`, `p_limit` | `commonMain/features/watching/sync/SupabaseProgressSyncAdapter.kt:73` |
| `sync_pull_watch_progress_delta` | `p_profile_id`, `p_since_event_id`, `p_limit` | `commonMain/features/watching/sync/SupabaseProgressSyncAdapter.kt:41` |
| `sync_pull_watched_items` | `p_profile_id`, `p_page`, `p_page_size` | `commonMain/features/watching/sync/SupabaseWatchedSyncAdapter.kt:69` |
| `sync_pull_watched_items_delta` | `p_profile_id`, `p_since_event_id`, `p_limit` | `commonMain/features/watching/sync/SupabaseWatchedSyncAdapter.kt:41` |
| `sync_push_addons` | `p_profile_id`, `p_addons` | `commonMain/features/addons/AddonRepository.kt:365` |
| `sync_push_collections` | `p_profile_id`, `p_collections_json` | `commonMain/features/collection/CollectionSyncService.kt:123` |
| `sync_push_home_catalog_settings` | `p_profile_id`, `p_platform`, `p_settings_json` | `commonMain/features/home/HomeCatalogSettingsSyncService.kt:160` |
| `sync_push_library_items` | `p_profile_id`, `p_items` | `commonMain/features/library/sync/SupabaseLibrarySyncAdapter.kt:79` |
| `sync_push_plugins` | `p_profile_id`, `p_plugins` | `fullCommonMain/features/plugins/PluginRepository.kt:521` |
| `sync_push_profile_settings_blob` | `p_profile_id`, `p_platform`, `p_settings_json` | `commonMain/core/sync/ProfileSettingsSync.kt:234` |
| `sync_push_profiles` | `p_client_max_profiles`, `p_profiles` | `commonMain/features/profiles/ProfileRepository.kt:208` |
| `sync_push_watch_progress` | `p_profile_id`, `p_entries` | `commonMain/features/watching/sync/SupabaseProgressSyncAdapter.kt:112` |
| `sync_push_watched_items` | `p_profile_id`, `p_items` | `commonMain/features/watching/sync/SupabaseWatchedSyncAdapter.kt:108` |

### Perfis e PIN

| Função | Parâmetros (heurístico) | Chamada em |
|---|---|---|
| `clear_profile_pin` | `p_profile_id`, `p_current_pin` | `commonMain/features/profiles/ProfileRepository.kt:374` |
| `clear_profile_pin_with_account_password` | `p_account_password`, `p_profile_id` | `commonMain/features/profiles/ProfileRepository.kt:391` |
| `set_profile_pin` | `p_profile_id`, `p_pin`, `p_current_pin` | `commonMain/features/profiles/ProfileRepository.kt:353` |
| `verify_profile_pin` | `p_profile_id`, `p_pin` | `commonMain/features/profiles/ProfileRepository.kt:329` |

### Login por dispositivo (TV) e sessões

| Função | Parâmetros (heurístico) | Chamada em |
|---|---|---|
| `poll_tv_login_session` | `p_code`, `p_device_nonce` | `commonMain/core/auth/DeviceLinkAuthRepository.kt:139` |
| `register_current_device` | não identificados | `commonMain/core/auth/DeviceSessionRegistration.kt:51` |
| `start_device_login_session` | `p_device_nonce`, `p_redirect_base_url`, `p_device_name`, `p_device_type` | `commonMain/core/auth/DeviceLinkAuthRepository.kt:114` |

### Watch party

| Função | Parâmetros (heurístico) | Chamada em |
|---|---|---|
| `watch_party_clock` | não identificados | `commonMain/features/party/PartyApi.kt:118` |
| `watch_party_enabled` | não identificados | `commonMain/features/party/PartyApi.kt:121` |
| `watch_party_list_messages` | `p_party_id`, `p_device_id`, `p_after` | `commonMain/features/party/PartyApi.kt:113` |
| `watch_party_preview` | `p_code` | `commonMain/features/party/PartyApi.kt:52` |
| `watch_party_send_message` | `p_party_id`, `p_device_id`, `p_body`, `p_client_id` | `commonMain/features/party/PartyApi.kt:103` |

### Avatares e assinatura de apoiadores (membership)

| Função | Parâmetros (heurístico) | Chamada em |
|---|---|---|
| `get_avatar_catalog` | não identificados | `commonMain/features/profiles/AvatarRepository.kt:167` |
| `get_member_profile_avatar_catalog` | não identificados | `commonMain/features/profiles/AvatarRepository.kt:190` |
| `get_member_profile_background_catalog` | não identificados | `commonMain/features/membership/ProfileBackgroundRepository.kt:53` |
| `get_my_member_access` | não identificados | `commonMain/features/membership/MemberAccessRemoteDataSource.kt:10` |
| `get_my_membership_overview` | não identificados | `commonMain/features/membership/MembershipOverviewRemoteDataSource.kt:11` |

## Tabelas lidas diretamente

- `addons` (`commonMain/features/addons/AddonRepository.kt:117`)
- `plugins` (`fullCommonMain/features/plugins/PluginRepository.kt:124`)

## Edge functions

- `delete-account` (`commonMain/core/auth/AuthRepository.kt:226`)
- `tv-logins-exchange` (`commonMain/core/auth/DeviceLinkAuthRepository.kt:176`)

## Buckets de storage

- `membership-profile-avatars` (`commonMain/features/profiles/AvatarRepository.kt:34`)
- `membership-profile-backgrounds` (`commonMain/features/membership/ProfileBackgroundRepository.kt:25`)

## Realtime

O watch party usa o canal Realtime do Supabase (`realtime-kt`).
