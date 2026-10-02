# AGENTS.md

Minecraft 1.7.10 Forge 向けの探索 Mod「Extraterrestrial Journey」（modid `etjourney`，パッケージ `chlorine.etjourney`）である．応答と文書は日本語で書く．

## ビルドとテスト

Git Bash で次の環境変数を設定してから実行する．

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
export GRADLE_USER_HOME="/c/gradle-home"
```

- `C:\gradle-home\gradle.properties` の `org.gradle.jvmargs` に `-Dfile.encoding=MS932` が必要である．無いと，非 ASCII パスのせいで test や checkstyle の worker が ClassNotFoundException で落ちる
- 整形とビルド：`./gradlew spotlessApply build`．spotless の整形違反は build を失敗させる
- 単体テスト：`./gradlew test --tests <FQCN>`（JUnit 5）
- 開発用クライアント：`./gradlew runClient`．ログは `run/client/logs/fml-client-latest.log` に出る
- Java 構文は Jabel（Java 8 バイトコード）である．`var` や switch 式は使えるが，`List.of` など Java 9 以降の API はテストでも使えない

## 層の規則

- 参照の向きは `core ← world ← content ← compat` の一方向に限る．`core` は core だけ，`world` は core・world，`content` は core・world・content，`compat` は全層を参照してよい
- 層のクラスから `ETJourney`・`Tags`・`proxy` を参照しない．`core` を別 Mod として切り出せる状態を保つためである
- 上の 2 つは `DependencyDirectionTest` が import と完全修飾名の両方で検査する．違反したらコードを直し，テストを緩めない
- 起動の順序は `proxy/CommonProxy` だけが持つ．`ETJourney` は FML のイベントを proxy に渡すだけにする
- 地域の中身は `content/<地域>/` に置く（例：`content/sunkensea/`）
- 連携先 Mod のクラスに触れてよいのは，`compat/<mod>/` の `CompatModule` 実装の内側だけである．新しい連携は `CompatManager.createDefault()` に登録する

## 依存の方針

- OKCore と GTNHLib には依存しない．compileOnly も使わない．欲しい部品は `core/` に自前で書く．GTNHLib は LGPL-3.0 なので，コードをコピーせず参考にとどめる
- UniMixins は，mixin が必要になった時点で前提 Mod に加えてよい
- 連携先の Mod は `compileOnly` か開発用の実行時依存として足し，必須の依存にしない
- `src/main/resources/pack.mcmeta` の `pack_format` は 4 以上に保つ．GTNHLib が同じパックに入っていると，4 未満の jar の blockstates JSON が無視されるためである

## コードの書き方

- 登録名は snake_case にし，`RegistryNames` を通してテクスチャ名と翻訳キーを作る．大文字は使わない
- 新しいクラスの javadoc は 1〜2 行にする．設計の理由はコミットメッセージと `docs/` に書く
- テクスチャは自作する．他の Mod のアセットを流用しない

## コミット

- 1 コミットには，意味の通る変更を 1 つだけ入れる．各コミットは単独で `build` が通る状態にする
- 件名は英語の命令形にする．本文は日本語で，「，」「．」を使う
