# Aviso de licença e atribuição

WATCHFIRST BASE é uma obra derivada do **Nuvio Mobile** (https://github.com/NuvioMedia/NuvioMobile), na tag `0.6.6-beta`, commit `2d52c8b71b598eb90e13fa9ec9ccff97ab601126`. O código original é de seus autores e está sob a **GNU General Public License v3.0** (arquivo `LICENSE`). Este projeto continua sob a mesma licença.

## O que a GPL-3.0 exige de quem distribui o app

- Entregar o código-fonte completo e correspondente (incluindo as modificações deste projeto) a quem receber o APK ou o IPA, sob a GPL-3.0.
- Manter os avisos de copyright e de licença.
- Indicar que o código foi modificado: veja `CHANGES.md` e o histórico do git.
- Não aplicar restrições adicionais ao que a GPL já concede.

Isto é um resumo, não aconselhamento jurídico.

## Marca

"Nuvio" e os logotipos do Nuvio são do projeto original. Eles foram removidos das telas, dos textos e dos ícones. A tela de licenças do app mantém o crédito ao Nuvio Mobile de propósito.

## Componentes de terceiros incluídos

| Item | Origem | Licença |
|---|---|---|
| Anton SC (`composeApp/src/commonMain/composeResources/font`) | Google Fonts | SIL OFL 1.1, texto em `watchfirst/licenses/OFL-AntonSC.txt` |
| `composeApp/libs/lib-nuvio-engine-android-0.1.6.aar` | NuvioMedia/nuvio-engine | GPL-3.0 |
| `composeApp/libs/nuvio-libass-release.aar` | NuvioMedia/nuvio-libass | **sem licença declarada no repositório** (baseado em Plezy e libass-android). Confirme antes de distribuir |
| `composeApp/libs/quickjs-kt-android-1.0.5-nuvio.aar` | fork de quickjs-kt | confirmar |
| `composeApp/libs/lib-*.aar` (Media3, decodificadores AV1, MPEG-H e FFmpeg) | AndroidX Media3 e extensões | Apache-2.0 para o Media3. O FFmpeg embutido tem licença própria (LGPL ou GPL conforme a configuração da build). Confirmar |
| MPVKit (submódulo, usado no iOS) | NuvioMedia/MPVKit | LGPL-3.0 |

Os demais componentes vêm de dependências Maven declaradas em `gradle/libs.versions.toml`, cada uma com a sua licença.
