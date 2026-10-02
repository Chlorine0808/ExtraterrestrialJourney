# Extraterrestrial Journey 基盤 設計

- 日付：2026-10-02
- 対象：プロジェクトの立ち上げ（空の Mod がロードされ，ディレクトリ構成が固まるところまで）

## 1. 目的と範囲

Extraterrestrial Journey（以下 ETJourney）は，Minecraft 1.7.10 Forge 向けの探索 Mod である．地上・地下・海・Nether・End を巡る「旅」を 1 本の Mod にまとめ，最後は End のボスで締める．モチーフは Terraria と Calamity Mod である．

構想中の地域は次のとおりである．中身は地域ごとに別の spec で設計する．

- Sunken Sea：砂漠の地下
- Abyss：深海
- Thaumcraft のバイオームとの連携
- Netherlicious の Crimson との連携
- End：4 本の柱と Cosmic のボス（`OmoshiroiKamo/run/BetterEnd_backport_plan.md` の構想を引き継ぐ）

本 spec の範囲は基盤だけである．完了条件は次の 2 つとする．

1. `gradlew build` が通る．依存方向テストもこれに含まれる
2. `runClient` で起動すると，Mods 一覧に Extraterrestrial Journey が表示される．あわせて，ログに preInit・init・postInit の通過が出る

## 2. 確定事項

| 項目 | 値 |
|---|---|
| 雛形 | GTNewHorizons/ExampleMod1.7.10（Kotlin DSL 版 buildscript，blowdryer 0.2.2） |
| 置き場所 | `minecraft/ExtraterrestrialJourney/` |
| modid | `etjourney` |
| 表示名 | Extraterrestrial Journey（略称 ETJourney） |
| パッケージ | `chlorine.etjourney` |
| ライセンス | MIT．テクスチャはすべて自作する |
| Java 構文 | `enableModernJavaSyntax = jabel`．構文だけを使い，stubs は使わない |
| 前提 Mod | Forge のみ．UniMixins は，必要になった時点で前提に加えてよい |
| 任意連携 | Thaumcraft，Netherlicious．入っているときだけ有効になる |
| コミットのメール | リポジトリ単位で GitHub noreply を設定する |

### 2.1 OKCore と GTNHLib に依存しない理由

上流の改変に巻き込まれないことを優先する．GTNHLib 0.11.52 を調べた結果，次の 3 点を根拠に依存を見送った．

- ETJourney の中心であるワールド生成・バイオーム・ディメンションのヘルパが存在しない
- 0.x 版で，パッチ版でも予告なしに API を削除する（例：`5e10ab8` の BlockState API）
- 全 `Block` 派生と `Tessellator` をグローバルに改変する．さらに UniMixins と GTNHExtLib を前提にする

欲しい部品（設定，ネットワーク，ノイズなど）は，自前の `core/` で書き直す．GTNHLib は LGPL-3.0 なので，コードはコピーせず参考にとどめる．

### 2.2 GTNHLib と同じパックに入る場合の対策

GTNHLib は，`pack.mcmeta` が無いか `pack_format` が 4 未満の jar から，`blockstates/*.json` を無視する．そこで ETJourney の jar には，`pack_format` 4 以上の `pack.mcmeta` を同梱する．

## 3. パッケージ構成

```
chlorine.etjourney
├─ ETJourney.java          @Mod 本体．起動の各段階を下の層へ順に配るだけ
├─ Tags.java               Gradle が生成（VERSION）
├─ proxy/                  CommonProxy, ClientProxy
├─ core/                   将来の切り出し候補．他の層を参照しない
│   ├─ config/             Forge Configuration の薄いラッパ
│   ├─ registry/           登録ヘルパ（modid 接頭辞を統一）
│   ├─ network/            SimpleNetworkWrapper のラッパ
│   └─ util/               ログなど
├─ world/                  地形生成エンジン（バイオーム ID の管理，生成器の登録口）
├─ content/                地域ごとの中身．基盤段階では入口の ModContent だけを置く
└─ compat/                 任意連携
    ├─ CompatManager.java
    ├─ thaumcraft/ThaumcraftCompat.java
    └─ netherlicious/NetherliciousCompat.java
```

resources は次の構成とする．

```
mcmod.info
pack.mcmeta                （pack_format 4）
LICENSE                    （MIT）
assets/etjourney/lang/en_US.lang
assets/etjourney/lang/ja_JP.lang
```

地域ごとのパッケージ（`content/sunkensea/` など）は，中身を作る時点で追加する．

### 3.1 依存の向き

参照は `core ← world ← content ← compat` の一方向に限る．`proxy` と `ETJourney` は，すべての層を参照してよい．

- `core` は `world`・`content`・`compat` を import しない
- `world` は `content`・`compat` を import しない
- `content` は `compat` を import しない

この規則は JUnit テスト（`DependencyDirectionTest`）で検査する．テストは `src/main/java` 配下のソースを走査し，違反する import が 1 つでもあれば失敗する．`core` をそのまま別 Mod に切り出せる状態を，このテストで保つ．

## 4. 起動の流れ

`ETJourney` は，FML の各段階を次の順で配る．基盤段階では，各メソッドは空か，ログを出すだけにする．

| 段階 | 順序 |
|---|---|
| preInit | Config の読み込み → core → content → world → compat の検出 → proxy |
| init | ネットワークの登録 → world（生成器の登録） → compat |
| postInit | compat（他 Mod のブロックやバイオームを参照する処理） |

`CompatManager` は `Loader.isModLoaded` で対象 Mod を検出し，入っているものだけ連携クラスを呼ぶ．modid は Thaumcraft が `Thaumcraft`，Netherlicious が `netherlicious` である．Netherlicious の modid は，実装時に jar の mcmod.info で確認する．連携クラスの中で相手 Mod のクラスを参照してよいのは，この分岐の内側だけである．

## 5. ビルド設定

- `gradle.properties` には次を設定する
  - `modId = etjourney`
  - `modName = Extraterrestrial Journey`
  - `modGroup = chlorine.etjourney`
  - `generateGradleTokenClass = chlorine.etjourney.Tags`
  - `enableModernJavaSyntax = jabel`
  - mixin・coremod・shadow はすべて無効にする
- `dependencies.gradle` は，テスト用の JUnit 以外を空にする．Thaumcraft と Netherlicious は，連携の中身を書く段階で `compileOnly` などとして足す
- ビルド環境の注意点は，README と AGENTS.md に書く．内容は OmoshiroiKamo と同じである
  - `GRADLE_USER_HOME=C:\gradle-home`
  - daemon の `file.encoding=MS932`
  - JAVA_HOME は jdk-21

## 6. 文書

- `README.md`：概要，前提 Mod，ビルド方法，ライセンス
- `AGENTS.md`（`CLAUDE.md` からも参照する）：ビルド環境，依存方向の規則，コーディング方針
- `docs/architecture.md`：層の役割，依存の向き，地域の構想一覧

## 7. 先送りにする判断

- **地下バイオームの実装方式**：1.7.10 のバイオームは縦の列単位で決まるので，地下だけを別のバイオームにはできない．populate で地形を作るか mixin を使うかは，Sunken Sea の spec で決める．mixin を使う場合は UniMixins を前提に加える
- **ブロックの描画方式**：当面はバニラのアイコン方式か ISBRH で描く．JSON モデルが大量に要る段階で改めて判断する
- **GitHub リポジトリの作成と公開**：基盤の完成後に確認する
