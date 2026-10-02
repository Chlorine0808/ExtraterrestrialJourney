# ETJourney の構成

ETJourney のコードの層，起動の流れ，地域の構想をまとめる．決定の経緯は `docs/superpowers/specs/2026-10-02-foundation-design.md` にある．

## 層

| 層 | パッケージ | 役割 | 参照してよい層 |
|---|---|---|---|
| core | `chlorine.etjourney.core` | 地域から独立した自前ライブラリ（設定，登録名，ネットワーク，ログ）．将来は別 Mod として切り出す候補である | core |
| world | `chlorine.etjourney.world` | 地形生成エンジン（バイオーム ID の管理，生成器の登録口） | core，world |
| content | `chlorine.etjourney.content` | 地域ごとのブロック，アイテム，エンティティ，バイオーム定義 | core，world，content |
| compat | `chlorine.etjourney.compat` | 任意連携．連携先の Mod が入っているときだけ動く | すべての層 |
| （ルート） | `ETJourney`，`Tags`，`proxy` | FML の入口と，起動順の管理 | すべての層 |

```
core  <-  world  <-  content  <-  compat
  ^         ^           ^           ^
  +---------+-----------+-----------+---- ETJourney / proxy
```

矢印は「参照される側 <- 参照する側」を表す．逆向きの参照と，層からルート（`ETJourney`，`Tags`，`proxy`）への参照は，`DependencyDirectionTest` が検出してビルドを失敗させる．

## 起動の流れ

`ETJourney` は FML のイベントを `CommonProxy` に渡すだけにする．各層を呼ぶ順序は `CommonProxy` が持つ．

| 段階 | 順序 |
|---|---|
| preInit | `ETJConfig.load` → `ModContent.preInit` → `ModWorld.preInit` → `CompatManager.preInit` |
| init | `ETJNetwork.init` → `ModWorld.init` → `CompatManager.init` |
| postInit | `CompatManager.postInit`（他の Mod のブロックやバイオームを参照する処理はここで行う） |

`CompatManager` は，最初の段階で `Loader.isModLoaded` を 1 度だけ呼んで連携先を検出する．入っている Mod の `CompatModule` だけに段階を渡す．

## 地域の構想

各地域は `content/<地域>/` に置く．連携が要る部分だけを `compat/<mod>/` に置く．

| 地域 | 場所 | パッケージ（予定） | 備考 |
|---|---|---|---|
| Sunken Sea | Overworld の砂漠の地下 | `content/sunkensea/` | Calamity Mod へのオマージュ |
| Abyss | Overworld の深海 | `content/abyss/` | Calamity Mod へのオマージュ |
| Thaumcraft 連携 | Thaumcraft のバイオーム | `compat/thaumcraft/` | Thaumcraft が入っているときだけ有効 |
| Crimson | Netherlicious の Crimson | `compat/netherlicious/` | Netherlicious が入っているときだけ有効 |
| End の柱 | End | `content/end/` | Terraria の Lunar イベント（4 本の柱）を下敷きにする |
| Cosmic | End | `content/end/` | 最終ボス．倒すとクリアになる |

End の構想は `OmoshiroiKamo/run/BetterEnd_backport_plan.md` から引き継ぐ．

## 先送りにした判断

- **地下バイオームの実装方式**：1.7.10 のバイオームは縦の列単位で決まるので，地下だけを別のバイオームにはできない．populate で地形を作るか mixin を使うかは，Sunken Sea の spec で決める．mixin を使う場合は UniMixins を前提に加える
- **ブロックの描画方式**：当面はバニラのアイコン方式か ISBRH で描く．JSON モデルが大量に要る段階で改めて判断する
- **GitHub リポジトリの作成と公開**：未定である
