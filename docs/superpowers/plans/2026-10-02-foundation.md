# ETJourney 基盤 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ExampleMod1.7.10 を雛形に，空の Mod `etjourney` が runClient でロードされ，`core ← world ← content ← compat` の層構成がテストで守られる状態を作る．

**Architecture:** `ETJourney`（@Mod）は FML の各段階を `CommonProxy` に渡し，proxy が core → content → world → compat の順に呼ぶ．`core` は切り出し可能な自前ライブラリ層であり，層間の参照方向は `DependencyDirectionTest` がソースを走査して検査する．任意連携は `CompatManager` が `Loader.isModLoaded` で判定し，入っている Mod の `CompatModule` だけを呼ぶ．

**Tech Stack:** Minecraft 1.7.10 / Forge 10.13.4.1614，GTNH gtnhconvention buildscript（settings plugin 2.0.33，Gradle 9.7.1），Jabel（Java 8 バイトコード），JUnit 5.10.2

**Spec:** `docs/superpowers/specs/2026-10-02-foundation-design.md`

## Global Constraints

- modid `etjourney`，表示名 `Extraterrestrial Journey`，パッケージ `chlorine.etjourney`
- `enableModernJavaSyntax = jabel`．mixin・coremod・shadow は無効
- 実行時の前提 Mod は Forge のみ．OKCore・GTNHLib に依存しない（compileOnly も含む）
- ライセンスは MIT（Copyright (c) 2026 Chlorine0808）
- `pack.mcmeta` の `pack_format` は 4
- 層の参照規則：`core`→core のみ，`world`→core・world，`content`→core・world・content，`compat`→core・world・content・compat．`ETJourney`・`Tags`・`proxy` は全層を参照してよいが，層からは参照されない
- 任意連携の modid：Thaumcraft は `Thaumcraft`，Netherlicious は `netherlicious`（jar の mcmod.info で確認済み）
- 新規クラスの javadoc は 1〜2 行に抑える
- ビルドは次の環境変数で実行する（Git Bash）：

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
export GRADLE_USER_HOME="/c/gradle-home"
cd "/c/My works/趣味のほう/minecraft/ExtraterrestrialJourney"
```

  `C:\gradle-home\gradle.properties` に daemon の `-Dfile.encoding=MS932` が設定済みである．これが無いと非 ASCII パスで worker が ClassNotFoundException で死ぬ．

## Review Focus

- 層のクラスを import ではなく完全修飾名で参照した場合も，依存方向テストは違反として検出する（Task 2 のテストで固定）
- 層から `ETJourney` や `Tags` のようなルート直下のクラスを参照した場合も違反とする．参照すると core を切り出せなくなるからである（Task 2 で固定）
- 大文字や空白を含む登録名は，テクスチャのパスと登録名の大小ずれの原因になる．`RegistryNames` は例外で拒否する（Task 3 で固定）
- 連携先の Mod が入っていない環境では，その連携クラスの段階メソッドは一切呼ばれない（Task 4 で固定）
- 連携先が 1 つも無い環境でも，起動は全段階を通過する（Task 4 のテストと Task 5 の runClient で確認）

---

## File Structure

```
ExtraterrestrialJourney/
├─ build.gradle.kts, settings.gradle.kts, gradle.properties, dependencies.gradle, repositories.gradle
├─ gradlew, gradlew.bat, gradle/, gtnhShared/, .editorconfig, .gitattributes, .gitignore, .java-version, jitpack.yml
├─ .github/workflows/build-and-test.yml
├─ LICENSE, README.md, AGENTS.md, CLAUDE.md, docs/architecture.md
└─ src/
   ├─ main/java/chlorine/etjourney/
   │  ├─ ETJourney.java                 @Mod．FML イベントを proxy に渡すだけ
   │  ├─ proxy/CommonProxy.java         層の呼び出し順を持つ
   │  ├─ proxy/ClientProxy.java         クライアント専用の登録口
   │  ├─ core/ModInfo.java              MODID・NAME 定数
   │  ├─ core/util/ModLog.java          共有 Logger
   │  ├─ core/config/ETJConfig.java     Forge Configuration の読み込み
   │  ├─ core/registry/RegistryNames.java  登録名の検証とテクスチャ名・翻訳キーの生成
   │  ├─ core/network/ETJNetwork.java   SimpleNetworkWrapper の保持
   │  ├─ world/ModWorld.java            地形生成エンジンの入口
   │  ├─ content/ModContent.java        地域コンテンツの入口
   │  ├─ compat/CompatModule.java       連携の共通インターフェース
   │  ├─ compat/CompatManager.java      検出と段階の配布
   │  ├─ compat/thaumcraft/ThaumcraftCompat.java
   │  └─ compat/netherlicious/NetherliciousCompat.java
   ├─ main/resources/
   │  ├─ mcmod.info, pack.mcmeta, LICENSE
   │  └─ assets/etjourney/lang/en_US.lang, ja_JP.lang
   └─ test/java/chlorine/etjourney/
      ├─ DependencyDirectionTest.java
      ├─ core/registry/RegistryNamesTest.java
      └─ compat/CompatManagerTest.java
```

---

### Task 1: ExampleMod から雛形を作り，空の Mod をビルドする

**Files:**
- Create: ExampleMod 由来のビルド一式（上の File Structure の 1〜3 行目）
- Create: `src/main/java/chlorine/etjourney/ETJourney.java`, `proxy/CommonProxy.java`, `proxy/ClientProxy.java`, `core/ModInfo.java`, `core/util/ModLog.java`
- Create: `src/main/resources/mcmod.info`, `pack.mcmeta`, `LICENSE`, `assets/etjourney/lang/en_US.lang`, `ja_JP.lang`
- Create: `LICENSE`

**Interfaces:**
- Produces: `chlorine.etjourney.core.ModInfo.MODID`（`"etjourney"`），`ModInfo.NAME`（`"Extraterrestrial Journey"`），`chlorine.etjourney.core.util.ModLog.LOG`（log4j `Logger`），`CommonProxy#preInit/init/postInit(FML*Event)`

- [ ] **Step 1: ExampleMod を取得し，必要なファイルだけを複写する**

```bash
EM=$(mktemp -d)/em
git clone -q --depth 1 https://github.com/GTNewHorizons/ExampleMod1.7.10.git "$EM"
cp -r "$EM"/{build.gradle.kts,settings.gradle.kts,gradle.properties,dependencies.gradle,repositories.gradle,gradlew,gradlew.bat,gradle,gtnhShared,.editorconfig,.gitattributes,.gitignore,.java-version,jitpack.yml} .
mkdir -p .github/workflows && cp "$EM"/.github/workflows/build-and-test.yml .github/workflows/
```

ExampleMod の `src/`，`README.md`，`docs/`，`CODEOWNERS`，`LICENSE*`，release 系 workflow は複写しない．

- [ ] **Step 2: `gradle.properties` を書き換える**

次のキーだけを変更する（他は ExampleMod の値のまま）：

```properties
modName = Extraterrestrial Journey
modId = etjourney
modGroup = chlorine.etjourney
generateGradleTokenClass = chlorine.etjourney.Tags
usesMavenPublishing = false
curseForgeProjectId =
```

`enableModernJavaSyntax = jabel`，`usesMixins = false`，`usesShadowedDependencies = false`，`coreModClass =` は ExampleMod の既定値のままで条件を満たすことを確認する．

- [ ] **Step 3: LICENSE を作る**

ルートの `LICENSE` と `src/main/resources/LICENSE` に，ExampleMod の `LICENSE-template` の本文を `Copyright (c) 2026 Chlorine0808` で書く．

- [ ] **Step 4: リソースを作る**

`src/main/resources/mcmod.info`：

```json
{
	"modListVersion": 2,
	"modList": [{
		"modid": "${modId}",
		"name": "${modName}",
		"description": "Exploration across the Overworld, the Nether and the End.",
		"version": "${modVersion}",
		"mcversion": "${minecraftVersion}",
		"url": "",
		"updateUrl": "",
		"authorList": ["Chlorine0808"],
		"credits": "",
		"logoFile": "",
		"screenshots": [],
		"parent": "",
		"requiredMods": [],
		"dependencies": [],
		"dependants": [],
		"useDependencyInformation": false
	}]
}
```

`src/main/resources/pack.mcmeta`：

```json
{
	"pack": {
		"pack_format": 4,
		"description": "Extraterrestrial Journey resources"
	}
}
```

`src/main/resources/assets/etjourney/lang/en_US.lang` と `ja_JP.lang`（中身は 1 行）：

```
# Extraterrestrial Journey
```

- [ ] **Step 5: core の定数と Logger を書く**

`src/main/java/chlorine/etjourney/core/ModInfo.java`：

```java
package chlorine.etjourney.core;

/** Mod-wide identifiers shared by every layer. */
public final class ModInfo {

    public static final String MODID = "etjourney";
    public static final String NAME = "Extraterrestrial Journey";

    private ModInfo() {}
}
```

`src/main/java/chlorine/etjourney/core/util/ModLog.java`：

```java
package chlorine.etjourney.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import chlorine.etjourney.core.ModInfo;

/** Shared logger for the whole mod. */
public final class ModLog {

    public static final Logger LOG = LogManager.getLogger(ModInfo.MODID);

    private ModLog() {}
}
```

- [ ] **Step 6: @Mod 本体と proxy を書く**

`src/main/java/chlorine/etjourney/ETJourney.java`：

```java
package chlorine.etjourney;

import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.proxy.CommonProxy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = ModInfo.MODID, name = ModInfo.NAME, version = Tags.VERSION, acceptedMinecraftVersions = "[1.7.10]")
public class ETJourney {

    @SidedProxy(
        clientSide = "chlorine.etjourney.proxy.ClientProxy",
        serverSide = "chlorine.etjourney.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
```

`src/main/java/chlorine/etjourney/proxy/CommonProxy.java`（Task 4 で層の呼び出しを足す）：

```java
package chlorine.etjourney.proxy;

import chlorine.etjourney.Tags;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Calls each layer in order for every FML lifecycle stage. */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ModLog.LOG.info("preInit done (version {})", Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        ModLog.LOG.info("init done");
    }

    public void postInit(FMLPostInitializationEvent event) {
        ModLog.LOG.info("postInit done");
    }
}
```

`src/main/java/chlorine/etjourney/proxy/ClientProxy.java`：

```java
package chlorine.etjourney.proxy;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Client-only registration such as renderers. */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }
}
```

- [ ] **Step 7: 整形してビルドする**

Run: `./gradlew spotlessApply build`
Expected: `BUILD SUCCESSFUL`．初回は依存の取得に数分かかる．

- [ ] **Step 8: jar の中身を確認する**

Run: `unzip -l build/libs/*-dev.jar 2>/dev/null || unzip -l $(ls build/libs/*.jar | grep -v sources | head -1)` の後，`unzip -p <jar> mcmod.info | grep '"modid"'`
Expected: `chlorine/etjourney/ETJourney.class`，`pack.mcmeta`，`assets/etjourney/lang/en_US.lang` が含まれ，mcmod.info の modid が `"etjourney"` に展開されている．

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "Scaffold the empty etjourney mod from ExampleMod1.7.10"
```

---

### Task 2: 層の参照方向をテストで固定する

**Files:**
- Modify: `dependencies.gradle`
- Modify: `build.gradle.kts`（JUnit Platform の有効化．convention が既に有効にしていれば不要）
- Test: `src/test/java/chlorine/etjourney/DependencyDirectionTest.java`

**Interfaces:**
- Produces: `DependencyDirectionTest.violations(Path sourceRoot)` が `List<String>`（`"<相対パス> -> <参照先>"` 形式）を返す．テスト内の static メソッドであり，main からは使わない

- [ ] **Step 1: JUnit を依存に足す**

`dependencies.gradle` の `dependencies { }` の中身を次にする：

```groovy
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.2'
    testImplementation 'org.junit.jupiter:junit-jupiter-params:5.10.2'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

`build.gradle.kts` の末尾に足す：

```kotlin
tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 2: 失敗するテストを書く**

`src/test/java/chlorine/etjourney/DependencyDirectionTest.java`：

```java
package chlorine.etjourney;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Keeps references flowing core <- world <- content <- compat so core stays extractable. */
class DependencyDirectionTest {

    private static final Map<String, Set<String>> ALLOWED = new HashMap<>();

    static {
        ALLOWED.put("core", layers("core"));
        ALLOWED.put("world", layers("core", "world"));
        ALLOWED.put("content", layers("core", "world", "content"));
        ALLOWED.put("compat", layers("core", "world", "content", "compat"));
    }

    private static final Pattern REFERENCE = Pattern.compile("chlorine\\.etjourney\\.(\\w+)");
    private static final Path ROOT_PACKAGE = Paths.get("chlorine", "etjourney");

    static List<String> violations(Path sourceRoot) throws IOException {
        Path base = sourceRoot.resolve(ROOT_PACKAGE);
        List<String> found = new ArrayList<>();
        if (!Files.isDirectory(base)) return found;
        try (Stream<Path> files = Files.walk(base)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                Path relative = base.relativize(file);
                if (relative.getNameCount() < 2) continue;
                Set<String> allowed = ALLOWED.get(relative.getName(0).toString());
                if (allowed == null) continue;
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Matcher m = REFERENCE.matcher(source.replaceFirst("(?m)^package\\s+[\\w.]+;", ""));
                Set<String> reported = new HashSet<>();
                while (m.find()) {
                    String target = m.group(1);
                    if (!allowed.contains(target) && reported.add(target)) {
                        found.add(relative.toString().replace('\\', '/') + " -> " + target);
                    }
                }
            }
        }
        return found;
    }

    @Test
    void mainSourcesFollowTheLayerRules() throws IOException {
        assertEquals(Collections.emptyList(), violations(Paths.get("src", "main", "java")));
    }

    @Test
    void coreImportingContentIsReported(@TempDir Path root) throws IOException {
        write(root, "core/Foo.java", "package chlorine.etjourney.core;\nimport chlorine.etjourney.content.Bar;\n");
        assertEquals(Collections.singletonList("core/Foo.java -> content"), violations(root));
    }

    @Test
    void fullyQualifiedReferenceIsReported(@TempDir Path root) throws IOException {
        write(root, "world/Gen.java", "package chlorine.etjourney.world;\nclass Gen { chlorine.etjourney.compat.X x; }\n");
        assertEquals(Collections.singletonList("world/Gen.java -> compat"), violations(root));
    }

    @Test
    void layerReferencingRootClassIsReported(@TempDir Path root) throws IOException {
        write(root, "core/Foo.java", "package chlorine.etjourney.core;\nimport chlorine.etjourney.Tags;\n");
        assertEquals(Collections.singletonList("core/Foo.java -> Tags"), violations(root));
    }

    @Test
    void downwardReferencesAndRootFilesAreAllowed(@TempDir Path root) throws IOException {
        write(root, "compat/A.java", "package chlorine.etjourney.compat;\nimport chlorine.etjourney.core.ModInfo;\nimport chlorine.etjourney.content.ModContent;\n");
        write(root, "ETJourney.java", "package chlorine.etjourney;\nimport chlorine.etjourney.compat.A;\n");
        write(root, "proxy/P.java", "package chlorine.etjourney.proxy;\nimport chlorine.etjourney.world.ModWorld;\n");
        assertTrue(violations(root).isEmpty());
    }

    private static void write(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(ROOT_PACKAGE).resolve(Paths.get("", relative.split("/")));
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private static Set<String> layers(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }
}
```

Jabel は Java 8 の API に対してコンパイルするので，テストでも `List.of` などの Java 9 以降の API は使わない．

- [ ] **Step 3: テストを実行する**

Run: `./gradlew test --tests chlorine.etjourney.DependencyDirectionTest`
Expected: 全 5 件 PASS．`violations` の実装をテストと同時に書いているので，最初から PASS する．失敗検出の確認として，`src/main/java/chlorine/etjourney/core/ModInfo.java` に一時的に `import chlorine.etjourney.Tags;` を足して再実行し，`mainSourcesFollowTheLayerRules` が `core/ModInfo.java -> Tags` で FAIL することを確かめる．確かめたら import を戻す．

- [ ] **Step 4: 整形してビルドする**

Run: `./gradlew spotlessApply build`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Enforce the core <- world <- content <- compat layer direction in tests"
```

---

### Task 3: core 層（設定，登録名，ネットワーク）

**Files:**
- Create: `src/main/java/chlorine/etjourney/core/config/ETJConfig.java`
- Create: `src/main/java/chlorine/etjourney/core/registry/RegistryNames.java`
- Create: `src/main/java/chlorine/etjourney/core/network/ETJNetwork.java`
- Test: `src/test/java/chlorine/etjourney/core/registry/RegistryNamesTest.java`

**Interfaces:**
- Consumes: `ModInfo.MODID`
- Produces:
  - `ETJConfig.load(java.io.File file)`：static．`ETJConfig.debugLogging`（`boolean`）を設定する
  - `RegistryNames.validate(String name)`：`String` を返す．`[a-z0-9_]+` 以外は `IllegalArgumentException`
  - `RegistryNames.texture(String name)`：`"etjourney:" + name` を返す
  - `RegistryNames.unlocalized(String name)`：`"etjourney." + name` を返す
  - `ETJNetwork.init()`：static．`ETJNetwork.channel()` が `SimpleNetworkWrapper` を返す（init 前は `IllegalStateException`），`ETJNetwork.nextId()` が `int`

- [ ] **Step 1: 失敗するテストを書く**

`src/test/java/chlorine/etjourney/core/registry/RegistryNamesTest.java`：

```java
package chlorine.etjourney.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class RegistryNamesTest {

    @Test
    void textureIsPrefixedWithTheModid() {
        assertEquals("etjourney:sunken_sand", RegistryNames.texture("sunken_sand"));
    }

    @Test
    void unlocalizedNameIsPrefixedWithTheModid() {
        assertEquals("etjourney.sunken_sand", RegistryNames.unlocalized("sunken_sand"));
    }

    @Test
    void validNameIsReturnedUnchanged() {
        assertEquals("abyss_stone_2", RegistryNames.validate("abyss_stone_2"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "SunkenSand", "sunken sand", "etjourney:sand", "sand-block" })
    void invalidNamesAreRejected(String name) {
        assertThrows(IllegalArgumentException.class, () -> RegistryNames.validate(name));
    }

    @Test
    void textureRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> RegistryNames.texture("Bad"));
    }
}
```

- [ ] **Step 2: テストが失敗することを確認する**

Run: `./gradlew test --tests chlorine.etjourney.core.registry.RegistryNamesTest`
Expected: コンパイルエラー（`RegistryNames` が無い）で FAIL

- [ ] **Step 3: `RegistryNames` を実装する**

```java
package chlorine.etjourney.core.registry;

import java.util.regex.Pattern;

import chlorine.etjourney.core.ModInfo;

/** Builds texture and translation names from one snake_case registry name. */
public final class RegistryNames {

    private static final Pattern VALID = Pattern.compile("[a-z0-9_]+");

    private RegistryNames() {}

    public static String validate(String name) {
        if (name == null || !VALID.matcher(name).matches()) {
            throw new IllegalArgumentException("Registry name must be snake_case [a-z0-9_]+: '" + name + "'");
        }
        return name;
    }

    public static String texture(String name) {
        return ModInfo.MODID + ":" + validate(name);
    }

    public static String unlocalized(String name) {
        return ModInfo.MODID + "." + validate(name);
    }
}
```

- [ ] **Step 4: テストが通ることを確認する**

Run: `./gradlew test --tests chlorine.etjourney.core.registry.RegistryNamesTest`
Expected: PASS（9 件）

- [ ] **Step 5: `ETJConfig` を書く**

Forge の `Configuration` は FML の起動データを参照するため単体テストでは生成できない．動作は Task 5 の runClient で `config/etjourney.cfg` の生成を見て確認する．

```java
package chlorine.etjourney.core.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Reads etjourney.cfg once during preInit. */
public final class ETJConfig {

    public static boolean debugLogging = false;

    private ETJConfig() {}

    public static void load(File file) {
        Configuration config = new Configuration(file);
        debugLogging = config.getBoolean(
            "debugLogging",
            Configuration.CATEGORY_GENERAL,
            false,
            "Log extra details for each lifecycle stage.");
        if (config.hasChanged()) {
            config.save();
        }
    }
}
```

- [ ] **Step 6: `ETJNetwork` を書く**

```java
package chlorine.etjourney.core.network;

import chlorine.etjourney.core.ModInfo;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/** Owns the mod's single packet channel; packets register against nextId(). */
public final class ETJNetwork {

    private static SimpleNetworkWrapper channel;
    private static int nextId = 0;

    private ETJNetwork() {}

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(ModInfo.MODID);
    }

    public static SimpleNetworkWrapper channel() {
        if (channel == null) throw new IllegalStateException("ETJNetwork.init() has not run yet");
        return channel;
    }

    public static int nextId() {
        return nextId++;
    }
}
```

- [ ] **Step 7: 整形してビルドする**

Run: `./gradlew spotlessApply build`
Expected: `BUILD SUCCESSFUL`．`DependencyDirectionTest` も PASS する

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "Add the core config, registry-name and network helpers"
```

---

### Task 4: world・content・compat の入口と起動の流れ

**Files:**
- Create: `src/main/java/chlorine/etjourney/world/ModWorld.java`
- Create: `src/main/java/chlorine/etjourney/content/ModContent.java`
- Create: `src/main/java/chlorine/etjourney/compat/CompatModule.java`
- Create: `src/main/java/chlorine/etjourney/compat/CompatManager.java`
- Create: `src/main/java/chlorine/etjourney/compat/thaumcraft/ThaumcraftCompat.java`
- Create: `src/main/java/chlorine/etjourney/compat/netherlicious/NetherliciousCompat.java`
- Modify: `src/main/java/chlorine/etjourney/proxy/CommonProxy.java`
- Test: `src/test/java/chlorine/etjourney/compat/CompatManagerTest.java`

**Interfaces:**
- Consumes: `ModLog.LOG`，`ETJConfig.load(File)`，`ETJNetwork.init()`
- Produces:
  - `interface CompatModule { String modId(); default void preInit() {} default void init() {} default void postInit() {} }`
  - `CompatManager(List<CompatModule> modules, java.util.function.Predicate<String> isLoaded)`，`static CompatManager createDefault()`，`List<CompatModule> active()`，`void preInit()`，`void init()`，`void postInit()`
  - `ModWorld.preInit()`，`ModWorld.init()`，`ModContent.preInit()`（すべて static）

- [ ] **Step 1: 失敗するテストを書く**

`src/test/java/chlorine/etjourney/compat/CompatManagerTest.java`：

```java
package chlorine.etjourney.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class CompatManagerTest {

    private static final class Recorder implements CompatModule {

        private final String modId;
        private final List<String> calls;

        Recorder(String modId, List<String> calls) {
            this.modId = modId;
            this.calls = calls;
        }

        @Override
        public String modId() {
            return modId;
        }

        @Override
        public void preInit() {
            calls.add(modId + ":preInit");
        }

        @Override
        public void init() {
            calls.add(modId + ":init");
        }

        @Override
        public void postInit() {
            calls.add(modId + ":postInit");
        }
    }

    @Test
    void onlyLoadedModsReceiveStages() {
        List<String> calls = new ArrayList<>();
        CompatManager manager = new CompatManager(
            Arrays.<CompatModule>asList(new Recorder("Thaumcraft", calls), new Recorder("netherlicious", calls)),
            "netherlicious"::equals);

        manager.preInit();
        manager.init();
        manager.postInit();

        assertEquals(Arrays.asList("netherlicious:preInit", "netherlicious:init", "netherlicious:postInit"), calls);
    }

    @Test
    void noLoadedModsMeansNoCallsAndNoErrors() {
        List<String> calls = new ArrayList<>();
        CompatManager manager = new CompatManager(
            Collections.<CompatModule>singletonList(new Recorder("Thaumcraft", calls)),
            id -> false);

        manager.preInit();
        manager.init();
        manager.postInit();

        assertEquals(Collections.emptyList(), calls);
        assertEquals(0, manager.active().size());
    }

    @Test
    void detectionHappensOncePerManager() {
        List<String> asked = new ArrayList<>();
        CompatManager manager = new CompatManager(
            Collections.<CompatModule>singletonList(new Recorder("Thaumcraft", new ArrayList<>())),
            id -> {
            asked.add(id);
            return true;
        });

        manager.preInit();
        manager.init();
        manager.postInit();

        assertEquals(Collections.singletonList("Thaumcraft"), asked);
    }
}
```

- [ ] **Step 2: テストが失敗することを確認する**

Run: `./gradlew test --tests chlorine.etjourney.compat.CompatManagerTest`
Expected: コンパイルエラー（`CompatModule`・`CompatManager` が無い）で FAIL

- [ ] **Step 3: `CompatModule` と `CompatManager` を実装する**

`compat/CompatModule.java`：

```java
package chlorine.etjourney.compat;

/** One optional integration; its stage methods run only when modId() is loaded. */
public interface CompatModule {

    String modId();

    default void preInit() {}

    default void init() {}

    default void postInit() {}
}
```

`compat/CompatManager.java`：

```java
package chlorine.etjourney.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import chlorine.etjourney.compat.netherlicious.NetherliciousCompat;
import chlorine.etjourney.compat.thaumcraft.ThaumcraftCompat;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.Loader;

/** Detects installed integration targets once and forwards lifecycle stages to them. */
public final class CompatManager {

    private final List<CompatModule> modules;
    private final Predicate<String> isLoaded;
    private List<CompatModule> active;

    public CompatManager(List<CompatModule> modules, Predicate<String> isLoaded) {
        this.modules = modules;
        this.isLoaded = isLoaded;
    }

    public static CompatManager createDefault() {
        return new CompatManager(
            Arrays.asList(new ThaumcraftCompat(), new NetherliciousCompat()),
            Loader::isModLoaded);
    }

    public List<CompatModule> active() {
        if (active == null) {
            List<CompatModule> found = new ArrayList<>();
            for (CompatModule module : modules) {
                if (isLoaded.test(module.modId())) {
                    found.add(module);
                    ModLog.LOG.info("Compat enabled: {}", module.modId());
                }
            }
            active = Collections.unmodifiableList(found);
        }
        return active;
    }

    public void preInit() {
        for (CompatModule module : active()) module.preInit();
    }

    public void init() {
        for (CompatModule module : active()) module.init();
    }

    public void postInit() {
        for (CompatModule module : active()) module.postInit();
    }
}
```

`compat/thaumcraft/ThaumcraftCompat.java`：

```java
package chlorine.etjourney.compat.thaumcraft;

import chlorine.etjourney.compat.CompatModule;

/** Thaumcraft biome integration; Thaumcraft classes may be touched only from here. */
public final class ThaumcraftCompat implements CompatModule {

    @Override
    public String modId() {
        return "Thaumcraft";
    }
}
```

`compat/netherlicious/NetherliciousCompat.java`：

```java
package chlorine.etjourney.compat.netherlicious;

import chlorine.etjourney.compat.CompatModule;

/** Netherlicious Crimson integration; Netherlicious classes may be touched only from here. */
public final class NetherliciousCompat implements CompatModule {

    @Override
    public String modId() {
        return "netherlicious";
    }
}
```

- [ ] **Step 4: テストが通ることを確認する**

Run: `./gradlew test --tests chlorine.etjourney.compat.CompatManagerTest`
Expected: PASS（3 件）

- [ ] **Step 5: world と content の入口を書く**

`world/ModWorld.java`：

```java
package chlorine.etjourney.world;

import chlorine.etjourney.core.util.ModLog;

/** Entry point of the terrain engine: biome IDs in preInit, generators in init. */
public final class ModWorld {

    private ModWorld() {}

    public static void preInit() {
        ModLog.LOG.debug("world: preInit");
    }

    public static void init() {
        ModLog.LOG.debug("world: init");
    }
}
```

`content/ModContent.java`：

```java
package chlorine.etjourney.content;

import chlorine.etjourney.core.util.ModLog;

/** Entry point that registers every region's blocks, items and entities. */
public final class ModContent {

    private ModContent() {}

    public static void preInit() {
        ModLog.LOG.debug("content: preInit");
    }
}
```

- [ ] **Step 6: `CommonProxy` で各層を順に呼ぶ**

`proxy/CommonProxy.java` を次に置き換える：

```java
package chlorine.etjourney.proxy;

import chlorine.etjourney.Tags;
import chlorine.etjourney.compat.CompatManager;
import chlorine.etjourney.content.ModContent;
import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.network.ETJNetwork;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.ModWorld;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Calls each layer in order for every FML lifecycle stage. */
public class CommonProxy {

    private final CompatManager compat = CompatManager.createDefault();

    public void preInit(FMLPreInitializationEvent event) {
        ETJConfig.load(event.getSuggestedConfigurationFile());
        ModContent.preInit();
        ModWorld.preInit();
        compat.preInit();
        ModLog.LOG.info("preInit done (version {})", Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        ETJNetwork.init();
        ModWorld.init();
        compat.init();
        ModLog.LOG.info("init done");
    }

    public void postInit(FMLPostInitializationEvent event) {
        compat.postInit();
        ModLog.LOG.info("postInit done");
    }
}
```

- [ ] **Step 7: 整形してビルドする**

Run: `./gradlew spotlessApply build`
Expected: `BUILD SUCCESSFUL`．テストは合計 3 クラスすべて PASS（`DependencyDirectionTest` が新しい層の参照も検査する）

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "Wire the world, content and compat entry points into the lifecycle"
```

---

### Task 5: runClient でロードを確認する

**Files:** なし（確認のみ）

- [ ] **Step 1: runClient をバックグラウンドで起動する**

Run（バックグラウンド，タイムアウト 15 分）：`./gradlew runClient > build/runClient.log 2>&1`

- [ ] **Step 2: ログで各段階の通過を確認する**

`run/client/logs/fml-client-latest.log`（無ければ `run/logs/` 配下か `build/runClient.log`）を grep する：

Run: `grep -E "\[etjourney\]|etjourney" <log> | grep -E "preInit done|init done|postInit done|Compat enabled"`
Expected: `preInit done (version ...)`，`init done`，`postInit done` の 3 行が出る．`Compat enabled` は出ない（連携先を入れていないため）．FML の状態表に `etjourney` が `UCHIJA`（全段階完了）で並ぶ．

- [ ] **Step 3: 設定ファイルの生成を確認する**

Run: `cat run/client/config/etjourney.cfg 2>/dev/null || find run -name etjourney.cfg -exec cat {} \;`
Expected: `B:debugLogging=false` を含む

- [ ] **Step 4: クライアントを終了する**

タイトル画面に到達したらプロセスを止める（バックグラウンドタスクを停止）．Mods 一覧の表示はユーザーに目視で確認してもらう．

---

### Task 6: 文書を整える

**Files:**
- Create: `README.md`, `AGENTS.md`, `CLAUDE.md`, `docs/architecture.md`

`project-docs` skill を使って書き，`validate_docs.py` で検証する．日本語は japanese-writing-style に従う．

- [ ] **Step 1: `README.md` を書く**

節：概要（Terraria・Calamity モチーフの探索 Mod），前提 Mod（Forge のみ．任意連携：Thaumcraft，Netherlicious），ビルド方法（Global Constraints の環境変数と `./gradlew build` / `runClient`），ライセンス（MIT，テクスチャは自作）．

- [ ] **Step 2: `AGENTS.md` と `CLAUDE.md` を書く**

`AGENTS.md`（200 行未満）：ビルド環境（GRADLE_USER_HOME，MS932，jdk-21），コマンド（`spotlessApply build`，`test --tests`，`runClient`），層の参照規則と `DependencyDirectionTest`，依存方針（OKCore・GTNHLib 不使用，UniMixins は必要時に可），登録名は `RegistryNames` を通すこと，javadoc は短く．`CLAUDE.md` は `@AGENTS.md` の 1 行だけにする．

- [ ] **Step 3: `docs/architecture.md` を書く**

層の役割表（core・world・content・compat・proxy），参照方向の図，起動の流れ（spec 4 節の表），地域の構想一覧（Sunken Sea，Abyss，Thaumcraft 連携，Crimson，End の柱，Cosmic）と，各地域を `content/<地域>/` に置く方針，spec 7 節の先送り事項．

- [ ] **Step 4: 検証してコミットする**

Run: `project-docs` skill の `validate_docs.py` を実行し，エラーが無いことを確認する．

```bash
git add -A
git commit -m "Document the build environment, layers and region roadmap"
```
