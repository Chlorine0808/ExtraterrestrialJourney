# Extraterrestrial Journey

地上・地下・海・Nether・End を巡って探索する，Minecraft 1.7.10 向けの Mod である（略称 ETJourney）．

モチーフは Terraria と Calamity Mod である．砂漠の地下の Sunken Sea，深海の Abyss，Thaumcraft や Netherlicious のバイオームとの連携，End の柱とボスを予定している．

## 前提 Mod

今のところなし

## セットアップ

JDK 21 で Gradle を起動する．Windows では，非 ASCII を含むパスで Gradle worker が落ちるのを防ぐため，`GRADLE_USER_HOME` の `gradle.properties` に `org.gradle.jvmargs=-Dfile.encoding=MS932` を設定しておく．

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
export GRADLE_USER_HOME="/c/gradle-home"
./gradlew build
```

## 使い方

`build/libs/` に出力された jar（`-dev` や `-sources` が付かないもの）を，Minecraft の `mods` フォルダに入れる．開発中は `./gradlew runClient` で開発用クライアントを起動できる．

### End の自動撮影

開発用クライアントに撮影リストを渡すと，End の地形を自動で撮影する．

```powershell
.\gradlew.bat runClient "-PetjShoot=shots/2026-10-28-end.txt"
```

- 撮影リストは `shots/` に置く．書式は `shots/2026-10-28-end.txt` の先頭のコメントにある．
- クライアントは 1920×1080 で起動し，クリエイティブのワールド `etj-shoot` を作るか読み込んで（シードが違えば作り直し，同じなら End だけ生成し直す），End へ飛んで撮影する．
- 結果は `run/client/etj-shots/<yyyyMMdd-HHmmss>/` に HTML と画像で出る．
- HTML は 1 件撮るごとに書き直す．全件が終わるとログに `[shoot] done <index.html のパス>` を出してクライアントを閉じる．10 分（または `settle-seconds` + 5 分）何も進まないか `budget-minutes` を超えたときも，残りを「時間切れ」にして書き出し，閉じる．
- `-PetjShoot` を付けなければ何も起きない．イベントの登録もしない．
- 撮った場所は `/etj end cam <x> <y> <z> <yaw> <pitch>` で再現できる．End に居なければ End へ送ってから，飛行と暗視を付けてその位置と向きに置く．

## ライセンス

MIT License，Copyright (c) 2026 Chlorine0808．
