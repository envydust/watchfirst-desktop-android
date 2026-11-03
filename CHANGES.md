# Alterações em relação ao Nuvio Mobile 0.6.6-beta

Data: 2026-11-03. Aviso de modificação exigido pela GPL-3.0.

## Identidade

- Android: `applicationId` `com.nuvio.app` para `com.watchfirst.app` (debug: `com.watchfirst.app.debug`). iOS: `PRODUCT_NAME` WATCHFIRST e bundle id `com.watchfirst.app`.
- Nome do app e textos: "Nuvio" virou "WATCHFIRST" em 1.065 trechos de 68 arquivos de strings (27 idiomas). Os créditos de licença ao Nuvio Mobile foram mantidos.
- Deep links: `nuvio://` virou `watchfirst://` (manifesto Android, Info.plist e widget do iOS, código e testes). O esquema `stremio://` continua valendo.
- Removido o App Link `https://nuvio.tv/party/`.
- Endereços do Nuvio (nuvio.tv, api.nuvio.tv, nuvioapp.space e a API de releases no GitHub) trocados por constantes em `WatchfirstLinks.kt`, com o domínio de teste `watchfirst.invalid`.
- Nome do cliente enviado a serviços externos (sessão de dispositivo, User-Agent do MDBList, Jellyfin/Emby, autorização debrid) trocado para WATCHFIRST.
- Defaults do build: URL do mural de apoiadores esvaziada e URIs de redirecionamento do Trakt e do Simkl em `watchfirst://`.

## Visual

- Tema padrão e único: preto com verde #24FF00 (paleta `ThemeColors.White`, exibida como "WATCHFIRST"). O seletor de temas fica escondido e qualquer tema salvo ou sincronizado vira o WATCHFIRST (`themeSelectionEnabled` em `AppFeaturePolicy`).
- Fonte Anton SC em `displayLarge`, `headlineLarge`, `displaySm` e `displayMd`.
- 96 imagens de marca regeneradas nos mesmos tamanhos (ícones Android e iOS, splash, wordmarks, ícones do seletor) e novo ícone de notificação.
- Cor de fundo da janela e do splash do Android: `#050805`.
- Corrigido após testar no emulador: o fundo roxo da tela de login e o botão "Sign In" branco agora seguem a marca (gradiente verde escuro e botão #24FF00); a cor padrão do avatar e da malha de fundo dos perfis passou de azul para #24FF00; o tema define a cor `tertiary` (antes vinha rosa do Material); os títulos em Anton SC usam espaçamento positivo (o negativo, feito para a fonte original, sobrepunha as letras).

## Funcionalidades (`AppFeaturePolicy`, em todas as distribuições)

Desligados: runtime de plugins, P2P, atualizador interno, doações, página de apoiadores, conexão com servidores customizados e seletor de temas.

## Removido

`.github/` (workflows e templates do projeto original), `store.json` (fonte AltStore do Nuvio), `CONTRIBUTING.md` e os scripts de ícones e de fonte de loja, que dependiam de arquivos que não estão no repositório.

## Adicionado

`README.md`, `NOTICE.md`, `CHANGES.md`, `local.properties.example`, `watchfirst/` (contrato do backend, licença da fonte e protótipo web), `WatchfirstLinks.kt` e a fonte `antonsc_regular.ttf`.

## Mantido de propósito

Pacote Kotlin `com.nuvio.app`, `rootProject.name = "Nuvio"` (define o pacote do código gerado de recursos), `namespace` `com.nuvio.android`, nomes internos como `NuvioTheme` e `Theme.Nuvio`, chaves de configuração `NUVIO_*`, o protocolo de descoberta de servidor (`/.well-known/nuvio`) e os binários em `composeApp/libs`. Renomear isso quebraria o código gerado e centenas de imports sem benefício visível.
