# WATCHFIRST BASE

Base do aplicativo WATCHFIRST para Android e iOS. É uma obra derivada do [Nuvio Mobile](https://github.com/NuvioMedia/NuvioMobile) 0.6.6-beta, feita em Kotlin Multiplatform com Compose Multiplatform e licenciada sob a GPL-3.0.

## Estado atual

- A troca de marca está aplicada (nome, identificadores, cores, fonte Anton SC, ícones, textos em 27 idiomas). O detalhe está em [CHANGES.md](CHANGES.md).
- **Testado:** compila (`:androidApp:assembleFullDebug`) e roda no emulador Android 17. Abre sem travar, o modo "Continue Without Account" funciona e as telas de login, perfis, início e configurações aparecem com a marca. **Não testado:** iOS, login com backend e reprodução de vídeo. Os testes automáticos (`:composeApp:testAndroidHostTest`) têm 1.869 aprovados e 15 pulados (recursos desligados). Quatro falham (ChapterSkipIntervalsTest, WatchProgressIdentityTest e DownloadSubtitlesTest) e não foram investigados, mas nenhum toca em código alterado pela troca de marca.
- **Backend:** sem um projeto Supabase o app funciona só no modo "Continue Without Account", com os dados guardados no aparelho. Entrar com conta, sincronizar e usar o watch party dependem de funções que não estão neste repositório. A lista completa está em [watchfirst/BACKEND-CONTRACT.md](watchfirst/BACKEND-CONTRACT.md).

## Para compilar

Android: Android Studio atual, JDK 17 e o SDK do Android com `compileSdk` 37. iOS: macOS com Xcode.

1. Copie `local.properties.example` para `local.properties` e preencha (o arquivo já está no `.gitignore`). O build falha se esse arquivo não existir, mesmo vazio.
2. Abra esta pasta no Android Studio, ou rode `gradlew.bat :androidApp:assembleFullDebug` no PowerShell ou no cmd.

No Windows, mantenha a pasta fora do OneDrive. O Git Bash não consegue chamar o `gradlew.bat` por causa dos espaços no nome da pasta. Se mais alguma ferramenta reclamar disso, renomeie para `WATCHFIRST-BASE`.

## Antes de publicar

- Trocar o domínio de teste `watchfirst.invalid` pelo real em `WatchfirstLinks.kt`, e recolocar o App Link no `AndroidManifest.xml` se quiser links https que abram o app.
- Publicar as páginas de termos e privacidade que o app abre.
- Trocar o ícone provisório (monograma WF). O seletor de ícone mostra seis opções idênticas.
- Criar o backend, usar chaves de API próprias e uma keystore própria para assinar.
- Revisar as licenças dos binários em `composeApp/libs`, listados em [NOTICE.md](NOTICE.md).
- O app tem só o tema verde WATCHFIRST: o seletor de temas do Nuvio está desligado. Para religar as outras cores, ponha `themeSelectionEnabled` em `true` nos arquivos `AppFeaturePolicy`.

## Fontes de conteúdo

A base não traz catálogos, canais nem fontes. O runtime de plugins e o P2P ficam desligados em `AppFeaturePolicy`, em todas as distribuições. Use apenas conteúdo que você tem direito de distribuir.

## Atualizar a partir do Nuvio

O remoto `upstream` aponta para o repositório original e o ramo principal é `main`. O primeiro commit é o código do Nuvio sem alterações e o segundo traz a troca de marca, então `git diff main~1 main` mostra tudo que mudou.

```bash
git fetch upstream --tags
git merge <tag-nova>   # resolva os conflitos de marca
```

## Referência de design

`watchfirst/web-prototype/watchfirst.html` é o protótipo web da interface (login, perfis, início e TV), com as cores e a fonte da marca.

## Licença

GPL-3.0, no arquivo [LICENSE](LICENSE). Veja [NOTICE.md](NOTICE.md) para as obrigações e os créditos.
