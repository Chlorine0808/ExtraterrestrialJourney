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

## ライセンス

MIT License，Copyright (c) 2026 Chlorine0808．
